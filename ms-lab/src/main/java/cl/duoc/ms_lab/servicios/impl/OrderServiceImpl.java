package cl.duoc.ms_lab.servicios.impl;

import cl.duoc.ms_lab.dtos.request.AssignOrderRequest;
import cl.duoc.ms_lab.dtos.request.OrderCreateRequest;
import cl.duoc.ms_lab.dtos.response.OrderResponse;
import cl.duoc.ms_lab.entidades.Laboratory;
import cl.duoc.ms_lab.entidades.Order;
import cl.duoc.ms_lab.entidades.OrderStatus;
import cl.duoc.ms_lab.exceptions.BadRequestException;
import cl.duoc.ms_lab.exceptions.ForbiddenException;
import cl.duoc.ms_lab.exceptions.NotFoundException;
import cl.duoc.ms_lab.mappers.LabOrderMapper;
import cl.duoc.ms_lab.repositorio.LaboratoryRepository;
import cl.duoc.ms_lab.repositorio.OrderRepository;
import cl.duoc.ms_lab.security.SecurityUtils;
import cl.duoc.ms_lab.servicios.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepo;
    private final LaboratoryRepository labRepo;
    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    public OrderServiceImpl(OrderRepository orderRepo, LaboratoryRepository labRepo) {
        this.orderRepo = orderRepo;
        this.labRepo = labRepo;
    }

    @Override
    public OrderResponse create(OrderCreateRequest req) {
        logger.info("Creando orden para el paciente: {}", req.patientId());
        Order o = new Order();
        o.setPatientId(req.patientId());
        o.setRequestedTest(req.requestedTest());
        o.setStatus(OrderStatus.CREATED);

        if (req.labCode() != null && !req.labCode().isBlank()) {
            logger.debug("Asignando laboratorio con código: {} a la nueva orden", req.labCode());
            Laboratory lab = labRepo.findByCode(req.labCode())
                    .orElseThrow(() -> new NotFoundException("Lab not found code=" + req.labCode()));
            o.setLaboratory(lab);
            o.setStatus(OrderStatus.ASSIGNED);
            o.setAssignedAt(LocalDateTime.now());
        }

        o = orderRepo.save(o);
        logger.debug("Orden creada con ID: {}", o.getId());
        return LabOrderMapper.toResponse(o);
    }

    @Override
    public OrderResponse assign(Long orderId, AssignOrderRequest req) {
        logger.info("Asignando orden con ID: {} al laboratorio: {}", orderId, req.labCode());
        Order o = orderRepo.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found id=" + orderId));

        Laboratory lab = labRepo.findByCode(req.labCode())
                .orElseThrow(() -> new NotFoundException("Lab not found code=" + req.labCode()));
        if (o.getStatus() == OrderStatus.FINISHED) {
            logger.warn("No se puede asignar una orden finalizada (ID: {})", orderId);
            throw new BadRequestException("Cannot assign a FINISHED order");
        }

        o.setLaboratory(lab);
        o.setStatus(OrderStatus.ASSIGNED);
        o.setAssignedAt(LocalDateTime.now());

        o = orderRepo.save(o);
        logger.debug("Orden con ID: {} asignada correctamente", o.getId());
        return LabOrderMapper.toResponse(o);
    }

    @Override
    public OrderResponse advanceToInProgress(Long orderId) {
        logger.info("Avanzando la orden con ID: {} a EN PROGRESO", orderId);
        Order o = orderRepo.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found id=" + orderId));

        if (o.getStatus() != OrderStatus.ASSIGNED) {
            logger.warn("Solo las órdenes ASIGNADAS pueden pasar a EN PROGRESO (ID de orden: {})", orderId);
            throw new BadRequestException("Only ASSIGNED orders can move to IN_PROGRESS");
        }
        o.setStatus(OrderStatus.IN_PROGRESS);
        o = orderRepo.save(o);
        logger.debug("Orden con ID: {} ahora está EN PROGRESO", o.getId());
        return LabOrderMapper.toResponse(o);
    }

    @Override
    public OrderResponse finish(Long orderId) {
        logger.info("Finalizando orden con ID: {}", orderId);
        Order o = orderRepo.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found id=" + orderId));

        if (o.getStatus() != OrderStatus.IN_PROGRESS) {
            logger.warn("Solo las órdenes EN PROGRESO pueden pasar a FINALIZADO (ID de orden: {})", orderId);
            throw new BadRequestException("Only IN_PROGRESS orders can move to FINISHED");
        }
        o.setStatus(OrderStatus.FINISHED);
        o = orderRepo.save(o);
        logger.debug("Orden con ID: {} FINALIZADA", o.getId());
        return LabOrderMapper.toResponse(o);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        logger.info("Buscando orden con ID: {}", id);
        Order o = orderRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found id=" + id));

        if (!SecurityUtils.hasRole("ADMIN")) {
            var ju = SecurityUtils.currentUserOrNull();
            String labCodeFromToken = ju != null ? ju.labCode() : null;
            String orderLabCode = (o.getLaboratory() != null) ? o.getLaboratory().getCode() : null;

            if (labCodeFromToken == null || orderLabCode == null || !labCodeFromToken.equals(orderLabCode)) {
                logger.warn("Acceso denegado: La orden {} no pertenece al laboratorio del usuario ({})", id, labCodeFromToken);
                throw new ForbiddenException("Order does not belong to your lab");
            }
        }

        logger.debug("Orden encontrada con ID: {}", o.getId());
        return LabOrderMapper.toResponse(o);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> list(OrderStatus status, String labCode, String patientId, Pageable pageable) {
        logger.info("Listando órdenes con estado: {}, código de laboratorio: {} y ID de paciente: {}", status, labCode, patientId);
        Page<Order> page;
        if (SecurityUtils.hasRole("ADMIN")) {
            logger.debug("El usuario es ADMIN, puede ver todas las órdenes");
            if (patientId != null && !patientId.isBlank()) {
                page = orderRepo.findByPatientId(patientId, pageable);
            } else if (status != null && labCode != null && !labCode.isBlank()) {
                Long labId = labRepo.findByCode(labCode).map(Laboratory::getId).orElse(-1L);
                page = orderRepo.findByStatusAndLaboratory_Id(status, labId, pageable);
            } else if (status != null) {
                page = orderRepo.findByStatus(status, pageable);
            } else if (labCode != null && !labCode.isBlank()) {
                Long labId = labRepo.findByCode(labCode).map(Laboratory::getId).orElse(-1L);
                page = orderRepo.findByLaboratory_Id(labId, pageable);
            } else {
                page = orderRepo.findAll(pageable);
            }
        } else {
            var ju = SecurityUtils.currentUserOrNull();
            String labCodeFromToken = ju != null ? ju.labCode() : null;
            if (labCodeFromToken == null || labCodeFromToken.isBlank())
                throw new ForbiddenException("Missing labCode in token");

            logger.debug("El usuario es LAB_TECH, forzando el código de laboratorio del token: {}", labCodeFromToken);
            Long labId = labRepo.findByCode(labCodeFromToken).map(Laboratory::getId).orElse(-1L);

            if (patientId != null && !patientId.isBlank()) {
                page = orderRepo.findByLaboratory_Id(labId, pageable);
            } else if (status != null) {
                page = orderRepo.findByStatusAndLaboratory_Id(status, labId, pageable);
            } else {
                page = orderRepo.findByLaboratory_Id(labId, pageable);
            }
        }
        logger.debug("Encontradas {} órdenes", page.getTotalElements());
        return page.map(LabOrderMapper::toResponse);
    }
}

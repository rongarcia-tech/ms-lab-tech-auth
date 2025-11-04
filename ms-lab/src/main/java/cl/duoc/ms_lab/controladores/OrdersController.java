package cl.duoc.ms_lab.controladores;

import cl.duoc.ms_lab.config.PageResponse;
import cl.duoc.ms_lab.dtos.request.AssignOrderRequest;
import cl.duoc.ms_lab.dtos.request.OrderCreateRequest;
import cl.duoc.ms_lab.dtos.response.OrderResponse;
import cl.duoc.ms_lab.entidades.OrderStatus;
import cl.duoc.ms_lab.servicios.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Orders")
@RestController
@RequestMapping("/orders")
public class OrdersController {

    private final OrderService service;
    private static final Logger logger = LoggerFactory.getLogger(OrdersController.class);

    public OrdersController(OrderService service) {
        this.service = service;
    }

    @Operation(summary = "Create order (ADMIN)")
    @PostMapping
    public OrderResponse create(@RequestBody OrderCreateRequest req) {
        logger.info("Creando orden para el paciente: {}", req.patientId());
        OrderResponse response = service.create(req);
        logger.debug("Orden creada con ID: {}", response.id());
        return response;
    }

    @Operation(summary = "Assign order to lab (ADMIN)")
    @PostMapping("/{id}/assign")
    public OrderResponse assign(@PathVariable Long id, @RequestBody AssignOrderRequest req) {
        logger.info("Asignando orden con ID: {} al laboratorio: {}", id, req.labCode());
        OrderResponse response = service.assign(id, req);
        logger.debug("Orden con ID: {} asignada correctamente", response.id());
        return response;
    }

    @Operation(summary = "Start order (ADMIN) → IN_PROGRESS")
    @PostMapping("/{id}/start")
    public OrderResponse start(@PathVariable Long id) {
        logger.info("Iniciando orden con ID: {}", id);
        OrderResponse response = service.advanceToInProgress(id);
        logger.debug("Orden con ID: {} iniciada", response.id());
        return response;
    }

    @Operation(summary = "Finish order (ADMIN) → FINISHED")
    @PostMapping("/{id}/finish")
    public OrderResponse finish(@PathVariable Long id) {
        logger.info("Finalizando orden con ID: {}", id);
        OrderResponse response = service.finish(id);
        logger.debug("Orden con ID: {} finalizada", response.id());
        return response;
    }

    @Operation(summary = "Get order by id (AUTH; LAB_TECH solo de su lab)")
    @GetMapping("/{id}")
    public OrderResponse getById(@PathVariable Long id) {
        logger.info("Buscando orden con ID: {}", id);
        OrderResponse response = service.getById(id);
        logger.debug("Orden encontrada con ID: {}", response.id());
        return response;
    }

    @Operation(summary = "List orders (AUTH; LAB_TECH solo de su lab)")
    @GetMapping
    public PageResponse<OrderResponse> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String labCode,     // ignorado para LAB_TECH en service
            @RequestParam(required = false) String patientId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        logger.info("Listando órdenes con estado: {}, código de laboratorio: {} y ID de paciente: {}", status, labCode, patientId);
        PageResponse<OrderResponse> response = PageResponse.from(service.list(status, labCode, patientId, pageable));
        logger.debug("Encontradas {} órdenes", response.totalElements());
        return response;
    }
}

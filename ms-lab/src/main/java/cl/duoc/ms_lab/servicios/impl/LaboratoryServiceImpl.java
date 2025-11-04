package cl.duoc.ms_lab.servicios.impl;

import cl.duoc.ms_lab.dtos.request.LabCreateRequest;
import cl.duoc.ms_lab.dtos.request.LabUpdateRequest;
import cl.duoc.ms_lab.dtos.response.LabResponse;
import cl.duoc.ms_lab.entidades.Laboratory;
import cl.duoc.ms_lab.exceptions.ConflictException;
import cl.duoc.ms_lab.exceptions.NotFoundException;
import cl.duoc.ms_lab.mappers.LabOrderMapper;
import cl.duoc.ms_lab.repositorio.LaboratoryRepository;
import cl.duoc.ms_lab.servicios.LaboratoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LaboratoryServiceImpl implements LaboratoryService {

    private final LaboratoryRepository repo;
    private static final Logger logger = LoggerFactory.getLogger(LaboratoryServiceImpl.class);

    public LaboratoryServiceImpl(LaboratoryRepository repo) {
        this.repo = repo;
    }

    @Override
    public LabResponse create(LabCreateRequest req) {
        logger.info("Creando laboratorio con código: {}", req.code());
        if (repo.existsByCode(req.code())) {
            logger.warn("El código de laboratorio ya existe: {}", req.code());
            throw new ConflictException("Laboratory code already exists: " + req.code());
        }
        Laboratory e = LabOrderMapper.toEntity(req);
        e = repo.save(e);
        logger.debug("Laboratorio creado con ID: {}", e.getId());
        return LabOrderMapper.toResponse(e);
    }

    @Override
    public LabResponse update(Long id, LabUpdateRequest req) {
        logger.info("Actualizando laboratorio con ID: {}", id);
        Laboratory e = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Laboratory not found id=" + id));

        if (req.code() != null && !req.code().equals(e.getCode()) && repo.existsByCode(req.code())) {
            logger.warn("El código de laboratorio ya existe: {}", req.code());
            throw new ConflictException("Laboratory code already exists: " + req.code());
        }

        LabOrderMapper.applyUpdate(e, req);
        e = repo.save(e);
        logger.debug("Laboratorio actualizado con ID: {}", e.getId());
        return LabOrderMapper.toResponse(e);
    }

    @Override
    @Transactional(readOnly = true)
    public LabResponse getById(Long id) {
        logger.info("Buscando laboratorio con ID: {}", id);
        return repo.findById(id)
                .map(lab -> {
                    logger.debug("Laboratorio encontrado con ID: {}", lab.getId());
                    return LabOrderMapper.toResponse(lab);
                })
                .orElseThrow(() -> {
                    logger.warn("No se encontró el laboratorio con ID: {}", id);
                    return new NotFoundException("Laboratory not found id=" + id);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LabResponse> list(Boolean active, Pageable pageable) {
        logger.info("Listando laboratorios con filtro activo: {}", active);
        Page<Laboratory> page = repo.findAll(pageable);
        logger.debug("Encontrados {} laboratorios", page.getTotalElements());
        return page.map(LabOrderMapper::toResponse);
    }

    @Override
    public LabResponse activate(Long id) {
        logger.info("Activando laboratorio con ID: {}", id);
        Laboratory e = repo.findById(id).orElseThrow(() -> new NotFoundException("Laboratory not found id=" + id));
        e.setActive("Y");
        repo.save(e);
        logger.debug("Laboratorio activado con ID: {}", e.getId());
        return LabOrderMapper.toResponse(e);
    }

    @Override
    public LabResponse deactivate(Long id) {
        logger.info("Desactivando laboratorio con ID: {}", id);
        Laboratory e = repo.findById(id).orElseThrow(() -> new NotFoundException("Laboratory not found id=" + id));
        e.setActive("N");
        repo.save(e);
        logger.debug("Laboratorio desactivado con ID: {}", e.getId());
        return LabOrderMapper.toResponse(e);
    }
}

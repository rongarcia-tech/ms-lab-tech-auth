package cl.duoc.ms_lab.controladores;

import cl.duoc.ms_lab.dtos.request.LabCreateRequest;
import cl.duoc.ms_lab.dtos.request.LabUpdateRequest;
import cl.duoc.ms_lab.dtos.response.LabResponse;
import cl.duoc.ms_lab.servicios.LaboratoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Laboratories")
@RestController
@RequestMapping("/labs")
public class LabsController {

    private final LaboratoryService service;
    private static final Logger logger = LoggerFactory.getLogger(LabsController.class);

    public LabsController(LaboratoryService service) {
        this.service = service;
    }

    @Operation(summary = "Create laboratory (ADMIN)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LabResponse create(@Valid @RequestBody LabCreateRequest req) {
        logger.info("Creando laboratorio con nombre: {}", req.name());
        LabResponse response = service.create(req);
        logger.debug("Laboratorio creado con ID: {}", response.id());
        return response;
    }

    @Operation(summary = "Update laboratory (ADMIN)")
    @PutMapping("/{id}")
    public LabResponse update(@PathVariable Long id, @Valid @RequestBody LabUpdateRequest req) {
        logger.info("Actualizando laboratorio con ID: {}", id);
        LabResponse response = service.update(id, req);
        logger.debug("Laboratorio actualizado con ID: {}", response.id());
        return response;
    }

    @Operation(summary = "Get laboratory by id (AUTH)")
    @GetMapping("/{id}")
    public LabResponse getById(@PathVariable Long id) {
        logger.info("Buscando laboratorio con ID: {}", id);
        LabResponse response = service.getById(id);
        logger.debug("Laboratorio encontrado con ID: {}", response.id());
        return response;
    }

    @Operation(summary = "List laboratories (AUTH)")
    @GetMapping
    public Page<LabResponse> list(
            @RequestParam(required = false) Boolean active,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        logger.info("Listando laboratorios con filtro activo: {}", active);
        Page<LabResponse> response = service.list(active, pageable);
        logger.debug("Encontrados {} laboratorios", response.getTotalElements());
        return response;
    }

    // Opcional: activar/desactivar de forma explícita (ADMIN)
    @Operation(summary = "Activate laboratory (ADMIN)")
    @PostMapping("/{id}/activate")
    public LabResponse activate(@PathVariable Long id) {
        logger.info("Activando laboratorio con ID: {}", id);
        LabResponse response = service.activate(id);
        logger.debug("Laboratorio activado con ID: {}", response.id());
        return response;
    }

    @Operation(summary = "Deactivate laboratory (ADMIN)")
    @PostMapping("/{id}/deactivate")
    public LabResponse deactivate(@PathVariable Long id) {
        logger.info("Desactivando laboratorio con ID: {}", id);
        LabResponse response = service.deactivate(id);
        logger.debug("Laboratorio desactivado con ID: {}", response.id());
        return response;
    }
}
package cl.duoc.ms_auth.servicios.impl;

import cl.duoc.ms_auth.dtos.RoleResponse;
import cl.duoc.ms_auth.repositorio.RoleRepository;
import cl.duoc.ms_auth.servicios.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementación del servicio de roles {@link RoleService}.
 * Proporciona la lógica de negocio para las operaciones relacionadas con los roles.
 */
@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepo;
    private static final Logger logger = LoggerFactory.getLogger(RoleServiceImpl.class);

    /**
     * Constructor para inyectar el repositorio de roles.
     *
     * @param roleRepo El repositorio para acceder a los datos de los roles.
     */
    public RoleServiceImpl(RoleRepository roleRepo) {
        this.roleRepo = roleRepo;
    }

    /**
     * Obtiene una lista de todos los roles disponibles en el sistema.
     * Mapea las entidades de rol a DTOs de respuesta de rol.
     *
     * @return una lista de {@link RoleResponse} que representa todos los roles.
     */
    @Override
    public List<RoleResponse> list() {
        logger.info("Obteniendo todos los roles.");
        List<RoleResponse> roles = roleRepo.findAll().stream()
                .map(r -> new RoleResponse(r.getId(), r.getName(), r.getDescription()))
                .toList();
        logger.debug("Se encontraron {} roles.", roles.size());
        return roles;
    }
}

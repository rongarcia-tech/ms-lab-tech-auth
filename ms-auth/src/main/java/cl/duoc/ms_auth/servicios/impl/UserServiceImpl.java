package cl.duoc.ms_auth.servicios.impl;

import cl.duoc.ms_auth.dtos.UserCreateRequest;
import cl.duoc.ms_auth.dtos.UserResponse;
import cl.duoc.ms_auth.dtos.UserUpdateRequest;
import cl.duoc.ms_auth.entidades.Role;
import cl.duoc.ms_auth.entidades.User;
import cl.duoc.ms_auth.exceptions.BadRequestException;
import cl.duoc.ms_auth.exceptions.ConflictException;
import cl.duoc.ms_auth.exceptions.NotFoundException;
import cl.duoc.ms_auth.mappers.UserMapperImpl;
import cl.duoc.ms_auth.repositorio.RoleRepository;
import cl.duoc.ms_auth.repositorio.UserRepository;
import cl.duoc.ms_auth.servicios.UserMapper;
import cl.duoc.ms_auth.servicios.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper mapper = new UserMapperImpl();
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    public UserServiceImpl(UserRepository userRepo, RoleRepository roleRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponse create(UserCreateRequest req) {
        logger.info("Iniciando creación de usuario con username: {}", req.username());
        if (userRepo.existsByUsername(req.username())) {
            logger.warn("Conflicto: El username '{}' ya está en uso.", req.username());
            throw new ConflictException("USERNAME en uso");
        }
        if (userRepo.existsByEmail(req.email())) {
            logger.warn("Conflicto: El email '{}' ya está en uso.", req.email());
            throw new ConflictException("EMAIL en uso");
        }

        Set<Role> roles = resolveRoles(req.roles());
        boolean isTech = roles.stream().anyMatch(r -> "LAB_TECH".equalsIgnoreCase(r.getName()));
        if (isTech && (req.labCode() == null || req.labCode().isBlank())) {
            logger.warn("Bad Request: LAB_CODE es obligatorio para el rol LAB_TECH.");
            throw new BadRequestException("LAB_CODE es obligatorio para LAB_TECH");
        }

        String hash = passwordEncoder.encode(req.password());
        User entity = mapper.toNewEntity(req, hash, roles);
        entity = userRepo.save(entity);
        logger.info("Usuario creado exitosamente con ID: {}", entity.getId());
        return mapper.toResponse(entity);
    }

    @Override
    public UserResponse update(Long id, UserUpdateRequest req) {
        logger.info("Iniciando actualización de usuario con ID: {}", id);
        User u = userRepo.findById(id).orElseThrow(() -> {
            logger.warn("Not Found: Usuario con ID '{}' no encontrado para actualizar.", id);
            return new NotFoundException("Usuario no encontrado");
        });

        Set<Role> roles = null;
        if (req.roles() != null) roles = resolveRoles(req.roles());

        String hash = null;
        if (req.password() != null) hash = passwordEncoder.encode(req.password());

        if (roles != null && roles.stream().anyMatch(r -> "LAB_TECH".equalsIgnoreCase(r.getName()))) {
            String finalLabCode = req.labCode() != null ? req.labCode() : u.getLabCode();
            if (finalLabCode == null || finalLabCode.isBlank()) {
                logger.warn("Bad Request: LAB_CODE es obligatorio para el rol LAB_TECH durante la actualización del usuario con ID: {}.", id);
                throw new BadRequestException("LAB_CODE es obligatorio para LAB_TECH");
            }
        }

        mapper.applyUpdate(u, req, hash, roles == null ? u.getRoles() : roles);
        u = userRepo.save(u);
        logger.info("Usuario con ID: {} actualizado exitosamente.", id);
        return mapper.toResponse(u);
    }

    @Override
    public void delete(Long id) {
        logger.info("Iniciando eliminación de usuario con ID: {}", id);
        User u = userRepo.findById(id).orElseThrow(() -> {
            logger.warn("Not Found: Usuario con ID '{}' no encontrado para eliminar.", id);
            return new NotFoundException("Usuario no encontrado");
        });
        userRepo.delete(u);
        logger.info("Usuario con ID: {} eliminado exitosamente.", id);
    }

    @Override
    public UserResponse getById(Long id) {
        logger.info("Buscando usuario por ID: {}", id);
        UserResponse response = userRepo.findById(id).map(mapper::toResponse)
                .orElseThrow(() -> {
                    logger.warn("Not Found: Usuario con ID '{}' no encontrado.", id);
                    return new NotFoundException("Usuario no encontrado");
                });
        logger.debug("Usuario con ID: {} encontrado.", id);
        return response;
    }

    @Override
    public List<UserResponse> list() {
        logger.info("Listando todos los usuarios.");
        List<UserResponse> users = userRepo.findAll().stream().map(mapper::toResponse).toList();
        logger.debug("Se encontraron {} usuarios.", users.size());
        return users;
    }

    @Override
    public UserResponse me(String username) {
        logger.info("Buscando información para el usuario autenticado: {}", username);
        User u = userRepo.findByUsername(username)
                .orElseThrow(() -> {
                    logger.warn("Not Found: Usuario autenticado '{}' no encontrado en la base de datos.", username);
                    return new NotFoundException("Usuario no encontrado");
                });
        logger.debug("Información para el usuario '{}' encontrada.", username);
        return mapper.toResponse(u);
    }

    private Set<Role> resolveRoles(List<String> names) {
        logger.debug("Resolviendo roles: {}", names);
        Set<Role> set = names.stream()
                .map(n -> roleRepo.findByName(n.toUpperCase(Locale.ROOT))
                        .orElseThrow(() -> {
                            logger.warn("Bad Request: Rol inválido '{}' solicitado.", n);
                            return new BadRequestException("Rol inválido: " + n);
                        }))
                .collect(Collectors.toSet());
        if (set.isEmpty()) {
            logger.warn("Bad Request: La lista de roles no puede estar vacía.");
            throw new BadRequestException("Debe indicar al menos un rol");
        }
        logger.debug("Roles resueltos exitosamente.");
        return set;
    }
}

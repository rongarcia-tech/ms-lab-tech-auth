package cl.duoc.ms_auth.controladores;

import cl.duoc.ms_auth.dtos.UserCreateRequest;
import cl.duoc.ms_auth.dtos.UserResponse;
import cl.duoc.ms_auth.dtos.UserUpdateRequest;
import cl.duoc.ms_auth.servicios.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * Controlador REST para gestionar las operaciones CRUD de los usuarios.
 */
@RestController
@RequestMapping("/users")
public class UsersController {

    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(UsersController.class);

    /**
     * Constructor para inyectar el servicio de usuarios.
     *
     * @param userService El servicio que maneja la lógica de negocio de los usuarios.
     */
    public UsersController(UserService userService){ this.userService = userService; }

    /**
     * Endpoint para crear un nuevo usuario.
     *
     * @param request El objeto {@link UserCreateRequest} con los datos del nuevo usuario.
     * @return Un {@link ResponseEntity} con el {@link UserResponse} del usuario creado.
     */
    @PostMapping
    public ResponseEntity<UserResponse> create(@RequestBody @Valid UserCreateRequest request) {
        logger.info("Solicitud recibida para crear un nuevo usuario.");
        UserResponse response = userService.create(request);
        logger.info("Usuario creado exitosamente con ID: {}", response.id());
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para actualizar un usuario existente.
     *
     * @param id El ID del usuario a actualizar.
     * @param request El objeto {@link UserUpdateRequest} con los nuevos datos del usuario.
     * @return Un {@link ResponseEntity} con el {@link UserResponse} del usuario actualizado.
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable Long id, @RequestBody @Valid UserUpdateRequest request) {
        logger.info("Solicitud recibida para actualizar el usuario con ID: {}", id);
        UserResponse response = userService.update(id, request);
        logger.info("Usuario con ID: {} actualizado exitosamente.", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para eliminar un usuario.
     *
     * @param id El ID del usuario a eliminar.
     * @return Un {@link ResponseEntity} con el estado no-content.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        logger.info("Solicitud recibida para eliminar el usuario con ID: {}", id);
        userService.delete(id);
        logger.info("Usuario con ID: {} eliminado exitosamente.", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint para obtener un usuario por su ID.
     *
     * @param id El ID del usuario a obtener.
     * @return Un {@link ResponseEntity} con el {@link UserResponse} del usuario encontrado.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        logger.info("Solicitud recibida para obtener el usuario con ID: {}", id);
        UserResponse response = userService.getById(id);
        logger.debug("Usuario con ID: {} encontrado.", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para listar todos los usuarios.
     *
     * @return Un {@link ResponseEntity} que contiene una lista de {@link UserResponse}.
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> list() {
        logger.info("Solicitud recibida para listar todos los usuarios.");
        List<UserResponse> users = userService.list();
        logger.debug("Se encontraron {} usuarios.", users.size());
        return ResponseEntity.ok(users);
    }

    /**
     * Endpoint para obtener la información del usuario autenticado actualmente.
     *
     * @param principal El objeto {@link Principal} que representa al usuario autenticado.
     * @return Un {@link ResponseEntity} con el {@link UserResponse} del usuario actual.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Principal principal) {
        logger.info("Solicitud recibida para obtener la información del usuario autenticado: {}", principal.getName());
        UserResponse response = userService.me(principal.getName());
        logger.debug("Información del usuario autenticado obtenida exitosamente.");
        return ResponseEntity.ok(response);
    }
}

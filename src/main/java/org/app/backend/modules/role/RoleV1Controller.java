package org.app.backend.modules.role;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.app.backend.common.response.ApiResponse;
import org.app.backend.common.response.DataApiResponse;
import org.app.backend.common.response.PagedApiResponse;
import org.app.backend.common.swagger.BadRequestApiResponse;
import org.app.backend.common.swagger.ForbiddenApiResponse;
import org.app.backend.common.swagger.NotFoundApiResponse;
import org.app.backend.common.swagger.UnauthorizedApiResponse;
import org.app.backend.modules.role.dto.RoleCreateDTO;
import org.app.backend.modules.role.dto.RoleDTO;
import org.app.backend.modules.role.dto.RoleDetailDTO;
import org.app.backend.modules.role.dto.RoleUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "02. Role (V1)", description = "API endpoints for retrieving and managing system roles")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleV1Controller {
  RoleService roleService;

  @Operation(
      summary = "Get paginated roles list",
      parameters = {
        @Parameter(name = "q"),
        @Parameter(name = "page"),
        @Parameter(name = "size"),
        @Parameter(name = "sort")
      },
      responses = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Roles list retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PagedApiResponseRoleDTO.class)))
      })
  @UnauthorizedApiResponse
  @ForbiddenApiResponse
  @BadRequestApiResponse
  @GetMapping
  public PagedApiResponse<RoleDTO> index(
      @RequestParam(defaultValue = "") String q, @Parameter(hidden = true) Pageable pageable) {
    Page<RoleDTO> roleDTOs = roleService.findAll(q, pageable);
    return PagedApiResponse.success(roleDTOs, RoleMessage.INDEX_SUCCESS.getMessage());
  }

  @Operation(
      summary = "Get role by ID",
      parameters = {@Parameter(name = "id", required = true)},
      responses = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Role retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = DataApiResponseRoleDetailDTO.class))),
      })
  @NotFoundApiResponse
  @UnauthorizedApiResponse
  @ForbiddenApiResponse
  @GetMapping("/{id}")
  public DataApiResponse<RoleDetailDTO> show(@PathVariable UUID id) {
    return DataApiResponse.success(roleService.findById(id), RoleMessage.SHOW_SUCCESS.getMessage());
  }

  @Operation(
      summary = "Create a new role",
      responses = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Role created successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class))),
      })
  @UnauthorizedApiResponse
  @ForbiddenApiResponse
  @BadRequestApiResponse
  @org.springframework.web.bind.annotation.ResponseStatus(
      org.springframework.http.HttpStatus.CREATED)
  @PostMapping
  public ApiResponse create(@RequestBody @Valid RoleCreateDTO dto) {
    roleService.create(dto);
    return ApiResponse.created(RoleMessage.CREATE_SUCCESS.getMessage());
  }

  @Operation(
      summary = "Update role by ID",
      parameters = {@Parameter(name = "id", required = true)},
      responses = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Role updated successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class)))
      })
  @NotFoundApiResponse
  @BadRequestApiResponse
  @UnauthorizedApiResponse
  @ForbiddenApiResponse
  @PutMapping("/{id}")
  public ApiResponse update(@PathVariable UUID id, @RequestBody @Valid RoleUpdateDTO dto) {
    roleService.update(id, dto);
    return ApiResponse.success(RoleMessage.UPDATE_SUCCESS.getMessage());
  }

  @Operation(
      summary = "Delete role by ID",
      parameters = {@Parameter(name = "id", required = true)},
      responses = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Role deleted successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class))),
      })
  @NotFoundApiResponse
  @UnauthorizedApiResponse
  @ForbiddenApiResponse
  @DeleteMapping("/{id}")
  public ApiResponse delete(@PathVariable UUID id) {
    roleService.delete(id);
    return ApiResponse.success(RoleMessage.DELETE_SUCCESS.getMessage());
  }

  public static class PagedApiResponseRoleDTO extends PagedApiResponse<RoleDTO> {}

  public static class DataApiResponseRoleDetailDTO extends DataApiResponse<RoleDetailDTO> {}
}

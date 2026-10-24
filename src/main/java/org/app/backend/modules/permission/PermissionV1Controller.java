package org.app.backend.modules.permission;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.app.backend.common.response.DataApiResponse;
import org.app.backend.common.response.PagedApiResponse;
import org.app.backend.common.swagger.BadRequestApiResponse;
import org.app.backend.common.swagger.ForbiddenApiResponse;
import org.app.backend.common.swagger.NotFoundApiResponse;
import org.app.backend.common.swagger.UnauthorizedApiResponse;
import org.app.backend.modules.permission.dto.PermissionDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
@Tag(
    name = "01. Permission (V1)",
    description = "API endpoints for retrieving and managing system permissions")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PermissionV1Controller {
  PermissionService permissionService;

  @Operation(
      summary = "Retrieve paginated list of permissions",
      parameters = {
        @Parameter(name = "q"),
        @Parameter(name = "page"),
        @Parameter(name = "size"),
        @Parameter(name = "sort")
      },
      responses = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Permissions list retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PagedApiResponsePermissionDTO.class)))
      })
  @GetMapping
  @BadRequestApiResponse
  @UnauthorizedApiResponse
  @ForbiddenApiResponse
  public PagedApiResponse<PermissionDTO> index(
      @RequestParam(defaultValue = "") String q, @Parameter(hidden = true) Pageable pageable) {
    return PagedApiResponse.success(
        permissionService.findAll(q, pageable), PermissionMessage.INDEX_SUCCESS.getMessage());
  }

  @Operation(
      summary = "Retrieve permission details by ID",
      parameters = {@Parameter(name = "id", required = true)},
      responses = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Permission retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = DataApiResponsePermissionDTO.class)))
      })
  @NotFoundApiResponse
  @UnauthorizedApiResponse
  @ForbiddenApiResponse
  @GetMapping("/{id}")
  public DataApiResponse<PermissionDTO> show(@PathVariable UUID id) {
    return DataApiResponse.success(
        permissionService.findById(id), PermissionMessage.SHOW_SUCCESS.getMessage());
  }

  public static class PagedApiResponsePermissionDTO extends PagedApiResponse<PermissionDTO> {}

  public static class DataApiResponsePermissionDTO extends DataApiResponse<PermissionDTO> {}
}

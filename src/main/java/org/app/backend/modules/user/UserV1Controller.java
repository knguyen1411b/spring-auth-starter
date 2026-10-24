package org.app.backend.modules.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.app.backend.modules.user.dto.UserCreateDTO;
import org.app.backend.modules.user.dto.UserDTO;
import org.app.backend.modules.user.dto.UserFilterDTO;
import org.app.backend.modules.user.dto.UserUpdateDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "03. User (V1)", description = "API endpoints for retrieving and managing system users")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserV1Controller {
  UserService userService;

  @Operation(
      summary = "Get a paginated list of users",
      parameters = {
        @Parameter(name = "q"),
        @Parameter(name = "active"),
        @Parameter(name = "locked"),
        @Parameter(name = "superuser"),
        @Parameter(name = "roleName"),
        @Parameter(name = "page"),
        @Parameter(name = "size"),
        @Parameter(name = "sort")
      },
      responses = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Users retrieved successfully",
            content =
                @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema =
                        @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = PagedApiResponseUserDTO.class))),
      })
  @BadRequestApiResponse
  @UnauthorizedApiResponse
  @ForbiddenApiResponse
  @GetMapping
  public PagedApiResponse<UserDTO> index(
      @Parameter(hidden = true) UserFilterDTO filter, @Parameter(hidden = true) Pageable pageable) {
    return PagedApiResponse.success(
        userService.findAll(filter, pageable), UserMessage.INDEX_SUCCESS.getMessage());
  }

  @Operation(
      summary = "Get user by ID",
      parameters = {@Parameter(name = "id", required = true)},
      responses = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User retrieved successfully",
            content =
                @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema =
                        @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = DataApiResponseUserDTO.class))),
      })
  @NotFoundApiResponse
  @UnauthorizedApiResponse
  @ForbiddenApiResponse
  @GetMapping("/{id}")
  public DataApiResponse<UserDTO> show(@PathVariable UUID id) {
    return DataApiResponse.success(userService.findById(id), UserMessage.SHOW_SUCCESS.getMessage());
  }

  @Operation(
      summary = "Create a new user",
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "User creation details",
              content =
                  @io.swagger.v3.oas.annotations.media.Content(
                      mediaType = "application/json",
                      schema =
                          @io.swagger.v3.oas.annotations.media.Schema(
                              implementation = UserCreateDTO.class))),
      responses = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "User created successfully",
            content =
                @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema =
                        @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = ApiResponse.class))),
      })
  @UnauthorizedApiResponse
  @ForbiddenApiResponse
  @BadRequestApiResponse
  @org.springframework.web.bind.annotation.ResponseStatus(
      org.springframework.http.HttpStatus.CREATED)
  @PostMapping
  public ApiResponse create(@RequestBody @Valid UserCreateDTO dto) {
    userService.create(dto);
    return ApiResponse.created(UserMessage.CREATE_SUCCESS.getMessage());
  }

  @Operation(
      summary = "Update user by ID",
      parameters = {@Parameter(name = "id", required = true)},
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "User update details",
              content =
                  @io.swagger.v3.oas.annotations.media.Content(
                      mediaType = "application/json",
                      schema =
                          @io.swagger.v3.oas.annotations.media.Schema(
                              implementation = UserUpdateDTO.class))),
      responses = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User updated successfully",
            content =
                @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema =
                        @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = ApiResponse.class)))
      })
  @UnauthorizedApiResponse
  @ForbiddenApiResponse
  @BadRequestApiResponse
  @NotFoundApiResponse
  @PatchMapping("/{id}")
  public ApiResponse update(@PathVariable UUID id, @RequestBody @Valid UserUpdateDTO dto) {
    userService.update(id, dto);
    return ApiResponse.success(UserMessage.UPDATE_SUCCESS.getMessage());
  }

  @Operation(
      summary = "Delete user by ID",
      parameters = {@Parameter(name = "id", required = true)},
      responses = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User deleted successfully",
            content =
                @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema =
                        @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = ApiResponse.class))),
      })
  @UnauthorizedApiResponse
  @ForbiddenApiResponse
  @NotFoundApiResponse
  @DeleteMapping("/{id}")
  public ApiResponse delete(@PathVariable UUID id) {
    userService.delete(id);
    return ApiResponse.success(UserMessage.DELETE_SUCCESS.getMessage());
  }

  public static class PagedApiResponseUserDTO extends PagedApiResponse<UserDTO> {}

  public static class DataApiResponseUserDTO extends DataApiResponse<UserDTO> {}
}

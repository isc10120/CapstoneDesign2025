package jamgaVOCA.demo.api

import jamgaVOCA.demo.api.annotation.AuthUser
import jamgaVOCA.demo.api.dto.ApiResponse
import jamgaVOCA.demo.domain.user.User
import jamgaVOCA.demo.service.AuthService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/v1/user")
class UserController (
    private val authService: AuthService
){
    @PostMapping("/sign-out")
    fun signOut(@AuthUser user: User): ApiResponse<Nothing> {
        authService.signOut(user.id!!)
        return ApiResponse.success(null)
    }
}
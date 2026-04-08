package jamgaVOCA.demo.api

import jamgaVOCA.demo.api.dto.ApiResponse
import jamgaVOCA.demo.api.dto.SignInRequest
import jamgaVOCA.demo.api.dto.SignInResponse
import jamgaVOCA.demo.api.dto.SignUpRequest
import jamgaVOCA.demo.service.AuthService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/sign-up")
    fun signUp(@RequestBody request: SignUpRequest): ApiResponse<Nothing> {
        authService.signUp(request)
        return ApiResponse.success(null)
    }

    @PostMapping("/sign-in")
    fun signIn(@RequestBody request: SignInRequest): ApiResponse<SignInResponse> {
        val data = authService.signIn(request)
        return ApiResponse.success(data)
    }
}

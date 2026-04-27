package jamgaVOCA.demo.infra.bootstrap

import jamgaVOCA.demo.domain.user.User
import jamgaVOCA.demo.domain.user.UserLevel
import jamgaVOCA.demo.domain.user.UserRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class DummyUserInitializer(
    private val userRepository: UserRepository
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        if (userRepository.findByIsDummyTrue() == null) {
            userRepository.save(
                User(
                    email = "",
                    password = "",
                    nickname = "dummy",
                    level = UserLevel.BEGINNER,
                    isDummy = true
                )
            )
        }
    }
}
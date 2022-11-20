package me.luvram.redissonsample

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.redisson.api.RMap
import org.redisson.api.RedissonClient
import org.redisson.codec.TypedJsonJacksonCodec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("develop", "test")
@SpringBootTest
class MapTest @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val client: RedissonClient
) {
    private val codec = TypedJsonJacksonCodec(User::class.java, User::class.java, objectMapper)
    val userCache = client.getMap<String, User>("USER", TypedJsonJacksonCodec(User::class.java, User::class.java, objectMapper))
    @Test
    fun `맵 put and get`() {
        // given
        val name = "ram"
        val age = 1
        userCache["test"] = User("1", name, age)

        // when
        val user: User = userCache["test"] ?: throw IllegalAccessError()

        // then
        assertThat(user.name).isEqualTo(name)
        assertThat(user.age).isEqualTo(age)
    }

}

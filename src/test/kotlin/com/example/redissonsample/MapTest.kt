package com.example.redissonsample

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.redisson.api.RMap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("develop", "test")
@SpringBootTest
class MapTest @Autowired constructor(
    val userCache: RMap<String, User>
) {
    @Test
    fun `맵 put and get`() {
        // given
        val name = "ram"
        val age = 1
        userCache["test"] = User(name, age)

        // when
        val user: User = userCache["test"] ?: throw IllegalAccessError()

        // then
        assertThat(user.name).isEqualTo(name)
        assertThat(user.age).isEqualTo(age)
    }

}
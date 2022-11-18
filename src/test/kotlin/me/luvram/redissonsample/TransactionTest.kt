package me.luvram.redissonsample

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("develop", "test")
@SpringBootTest
class TransactionTest @Autowired constructor(
    private val service: TransactionService
) {
    @Test
    fun `transaction`() {
        service.saveUser(User("id-1", "name-1", 1))
    }

    @Test
    fun `batch`() {
        service.saveUserBatch(User("id-2", "name-2", 2))
    }

    @Test
    fun `multi lock`() {
        service.saveUserWithLock(User("id-3", "name-3", 3))
    }
}

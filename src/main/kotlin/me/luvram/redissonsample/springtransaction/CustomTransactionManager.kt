package me.luvram.redissonsample.springtransaction

import me.luvram.redissonsample.batch.RedissonBatchManager
import org.apache.juli.logging.LogFactory
import org.redisson.api.BatchOptions
import org.redisson.api.BatchOptions.ExecutionMode
import org.redisson.api.RBatch
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import org.redisson.spring.transaction.RedissonTransactionObject
import org.redisson.transaction.TransactionException
import org.springframework.transaction.NoTransactionException
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionSystemException
import org.springframework.transaction.support.AbstractPlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionDefinition
import org.springframework.transaction.support.DefaultTransactionStatus
import org.springframework.transaction.support.ResourceTransactionManager
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.concurrent.TimeUnit


class CustomTransactionManager(
    private val client: RedissonClient
): AbstractPlatformTransactionManager(), ResourceTransactionManager {

    companion object {
        val log = LogFactory.getLog(this.javaClass)
    }

    init {
        isNestedTransactionAllowed = true
    }

    override fun doGetTransaction(): Any {
        val transactionObject = CustomTransactionObject()
        val holder = TransactionSynchronizationManager.getResource(client) as CustomTransactionHolder?
        if (holder != null) {
            transactionObject.transactionHolder = holder

        }
        return transactionObject

    }

    override fun isExistingTransaction(transaction: Any): Boolean {
        val transactionObject = transaction as CustomTransactionObject
        return transactionObject.transactionHolder != null
    }

    override fun doBegin(transaction: Any, definition: TransactionDefinition) {
        val transactionObject = transaction as CustomTransactionObject

        if (transactionObject.transactionHolder == null) {
            val batchOption = BatchOptions.defaults().executionMode(ExecutionMode.REDIS_WRITE_ATOMIC)
            val batch = client.createBatch(batchOption)
            val holder = CustomTransactionHolder(batch)
            transactionObject.transactionHolder = holder
            TransactionSynchronizationManager.bindResource(client, holder)
        }

    }

    override fun doCommit(status: DefaultTransactionStatus) {
        val transactionObject = status.transaction as CustomTransactionObject

        try {
            val holder = transactionObject.transactionHolder ?: throw TransactionSystemException("transaction is not exist")
            holder.batch.execute()
        } catch (e: TransactionException) {
            log.error(e)
            throw TransactionSystemException("Unable to commit transaction", e)
        }

    }

    override fun doRollback(status: DefaultTransactionStatus) {
        val transactionObject = status.transaction as CustomTransactionObject
        try {
            val holder = transactionObject.transactionHolder ?: throw TransactionSystemException("transaction is not exist")
            holder.batch.discard()
        } catch (e: TransactionException) {
            throw TransactionSystemException("Unable to rollback transaction", e)
        }
    }

    override fun getResourceFactory(): Any {
        return client
    }

    override fun doCleanupAfterCompletion(transaction: Any) {
        log.debug("doCleanupAfterCompletion")
        TransactionSynchronizationManager.unbindResourceIfPossible(client)
        val transactionObject = transaction as CustomTransactionObject
        transactionObject.transactionHolder = null
    }

    fun getCurrentTransaction(): RBatch {
        val to = TransactionSynchronizationManager.getResource(client) as CustomTransactionHolder?
            ?: throw NoTransactionException("No transaction is available for the current thread")
        return to.batch
    }

    fun <T> transaction(block: (batch: RBatch) -> T): T {
        val definition = DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED);
        val status = getTransaction(definition)
        val batch = getCurrentTransaction()
        try {
            val result = block(batch)
            commit(status)
            return result
        } catch(e: Exception) {
            rollback(status)
            throw e
        }
    }

    fun <T> transaction(vararg locks: RLock, block: (batch: RBatch) -> T): T {
        val multiLock = client.getMultiLock(*locks)

        if (multiLock.tryLock(1000, TimeUnit.MILLISECONDS)) {
            try {
                log.debug("transaction with lock")
                return transaction(block)
            } finally {
                multiLock.unlock()
            }
        } else {
            log.warn("Fail to get lock")
        }

        throw TransactionSystemException("something wrong")
    }
}

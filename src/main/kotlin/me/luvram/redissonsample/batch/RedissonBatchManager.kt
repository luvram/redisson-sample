package me.luvram.redissonsample.batch

import me.luvram.redissonsample.springtransaction.CustomTransactionManager
import org.apache.juli.logging.LogFactory
import org.redisson.api.BatchOptions
import org.redisson.api.BatchOptions.ExecutionMode
import org.redisson.api.RBatch
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import org.redisson.transaction.TransactionException
import org.springframework.transaction.NoTransactionException
import org.springframework.transaction.TransactionSystemException
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.concurrent.TimeUnit

class RedissonBatchManager(
    private val client: RedissonClient
) {
    companion object {
        val log = LogFactory.getLog(this.javaClass)
    }

    fun <T> runMulti(vararg locks: RLock, block: (batch: RBatch) -> T): T {
        val multiLock = client.getMultiLock(*locks)

        if (multiLock.tryLock(1000, TimeUnit.MILLISECONDS)) {
            try {
                return runMulti(block)
            } finally {
                multiLock.unlock()
            }
        } else {
            log.warn { "Fail to get lock" }
        }

        throw TransactionSystemException("something wrong")
    }

    fun <T> runMulti(block: (batch: RBatch) -> T): T {
        val batch = doGetBatch()
        try {
            doBegin(batch)
            val holder = batch.batchHolder ?: throw TransactionSystemException("batch is not exist")
            val result = block(holder.batch)
            doExecute(batch)
            return result
        } catch (e: Exception) {
            doDiscard(batch)
        } finally {
            doCleanupAfterCompletion(batch)
        }

        throw TransactionSystemException("something wrong")
    }

    private fun doGetBatch(): CustomBatchObject {
        log.debug("doGetBatch")
        val batchObject = CustomBatchObject()
        val holder = TransactionSynchronizationManager.getResource(client) as CustomBatchHolder?
        if (holder != null) {
            batchObject.batchHolder = holder

        }
        return batchObject
    }

    private fun doBegin(batchObject: CustomBatchObject) {
        log.debug("doBegin")
        if (batchObject.batchHolder == null) {
            val batchOption = BatchOptions.defaults().executionMode(ExecutionMode.REDIS_WRITE_ATOMIC)
            val batch = client.createBatch(batchOption)
            val holder = CustomBatchHolder(batch)
            batchObject.batchHolder = holder
            TransactionSynchronizationManager.bindResource(client, holder)
        }
    }

    private fun doExecute(batchObject: CustomBatchObject) {
        log.debug("doCommit")
        try {
            val holder = batchObject.batchHolder ?: throw TransactionSystemException("batch is not exist")
            holder.batch.execute()
        } catch (e: TransactionException) {
            CustomTransactionManager.log.error(e)
            throw TransactionSystemException("Unable to execute batch", e)
        }

    }

    private fun doDiscard(batchObject: CustomBatchObject) {
        log.debug("doRollback")
        try {
            val holder = batchObject.batchHolder ?: throw TransactionSystemException("batch is not exist")
            holder.batch.discard()
        } catch (e: TransactionException) {
            throw TransactionSystemException("Unable to discard batch", e)
        }
    }

    private fun doCleanupAfterCompletion(batch: CustomBatchObject) {
        log.debug("doCleanupAfterCompletion")
        TransactionSynchronizationManager.unbindResourceIfPossible(client)
        batch.batchHolder = null
    }

    fun getCurrentBatch(): RBatch {
        val holder = TransactionSynchronizationManager.getResource(client) as CustomBatchHolder?
            ?: throw NoTransactionException("No batch is available for the current thread")
        return holder.batch
    }
}

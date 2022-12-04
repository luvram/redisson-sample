package me.luvram.redissonsample.springtransaction

import org.apache.juli.logging.LogFactory
import org.redisson.api.BatchOptions
import org.redisson.api.BatchOptions.ExecutionMode
import org.redisson.api.RBatch
import org.redisson.api.RedissonClient
import org.redisson.transaction.TransactionException
import org.springframework.transaction.NoTransactionException
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionSystemException
import org.springframework.transaction.support.AbstractPlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionStatus
import org.springframework.transaction.support.ResourceTransactionManager
import org.springframework.transaction.support.TransactionSynchronizationManager


class CustomTransactionManager(
    private val client: RedissonClient
): AbstractPlatformTransactionManager(), ResourceTransactionManager {

    companion object {
        val log = LogFactory.getLog(this.javaClass)
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

    fun getCurrentTransaction(): RBatch {
        val to = TransactionSynchronizationManager.getResource(client) as CustomTransactionHolder?
            ?: throw NoTransactionException("No transaction is available for the current thread")
        return to.batch
    }

}

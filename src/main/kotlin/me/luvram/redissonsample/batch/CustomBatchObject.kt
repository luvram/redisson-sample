package me.luvram.redissonsample.batch

import org.springframework.transaction.support.SmartTransactionObject

class CustomBatchObject: SmartTransactionObject {
    var batchHolder: CustomBatchHolder? = null
    private var isRollbackOnly = false

    override fun flush() {
        // nothing to do
    }

    override fun isRollbackOnly(): Boolean {
        return isRollbackOnly
    }
}

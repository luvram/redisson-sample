package me.luvram.redissonsample.springtransaction

import org.springframework.transaction.support.SmartTransactionObject

class CustomTransactionObject: SmartTransactionObject {
    var transactionHolder: CustomTransactionHolder? = null
    private var isRollbackOnly = false

    override fun flush() {
        // nothing to do
    }

    override fun isRollbackOnly(): Boolean {
        return isRollbackOnly
    }
}

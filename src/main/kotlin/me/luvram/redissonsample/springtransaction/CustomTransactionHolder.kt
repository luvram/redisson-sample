package me.luvram.redissonsample.springtransaction

import org.redisson.api.RBatch

class CustomTransactionHolder(
    val batch: RBatch
)

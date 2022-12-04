package me.luvram.redissonsample.batch

import org.junit.jupiter.api.Assertions.*

internal class RedissonBatchManagerTest {
    // lock 이 하나일때 체크
    // lock이 여러개일 때 체크
    // 동시에 여러 스레드에서 사용하여도 에러 없는지, 배치가 서로 간섭되지는 않는지 체크
    // block에서 그냥 리턴했을때도 리소스가 해제되는지 체크
    // 에러났을 때 discard가 제대로 되는지 체크
}

package extend.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author 田奇杭
 * @Description 分布式锁类型枚举
 * @Date 2023/5/28 22:03
 */
@Getter
@AllArgsConstructor
public enum DistributedLockTypeEnum {

    /**
     * 普通分布式锁（单节点）
     */
    ORDINARY(1, "ordinaryDistributedLock", "普通分布式锁"),

    ;

    /**
     * 分布式锁类型
     */
    private final Integer distributedLockType;

    /**
     * 分布式锁名称
     */
    private final String distributedLockName;

    /**
     * 分布式锁类型描述
     */
    private final String distributedLockTypeDesc;

}

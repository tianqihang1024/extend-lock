package extend.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DistributedLockTypeEnum {

    /**
     * 单节点分布式锁
     */
    ORDINARY(1, "ordinary:%s", "单节点分布式锁"),

    ;

    /**
     * 分布式锁类型
     */
    private final Integer distributedType;

    /**
     * 分布式锁名称前缀
     */
    private final String distributedPrefix;

    /**
     * 分布式锁类型描述
     */
    private final String distributedTypeDesc;


    public String getDistributedLockName(String key) {
        return String.format(this.distributedPrefix, key);
    }

}

package extend.listener;

import jdk.internal.vm.annotation.ReservedStackAccess;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractOwnableSynchronizer;
import java.util.concurrent.locks.LockSupport;

/**
 * @author 田奇杭
 * @Description 同步队列（数据模型参考AQS）
 * @Date 2023/5/15 22:08
 */
public class SyncQueue extends AbstractOwnableSynchronizer {

    // VarHandle mechanics
    private static final VarHandle HEAD;
    private static final VarHandle TAIL;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            HEAD = l.findVarHandle(SyncQueue.class, "head", Node.class);
            TAIL = l.findVarHandle(SyncQueue.class, "tail", Node.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }

        // Reduce the risk of rare disastrous classloading in first call to
        // LockSupport.park: https://bugs.openjdk.java.net/browse/JDK-8074773
        Class<?> ensureLoaded = LockSupport.class;
    }

    /**
     * Head of the wait queue, lazily initialized.  Except for
     * initialization, it is modified only via method setHead.  Note:
     * If head exists, its waitStatus is guaranteed not to be
     * CANCELLED.
     */
    private transient volatile Node head;
    /**
     * Tail of the wait queue, lazily initialized.  Modified only via
     * method enq to add new wait node.
     */
    private transient volatile Node tail;

    /**
     * 检查并更新前置节点的状态。若线程应该阻塞，则返回 true
     * 硬指标：要求 pred == node.prev
     *
     * @param pred 当前节点的前置节点
     * @param node 当前节点
     * @return true：线程需要阻塞休眠
     */
    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        int ws = pred.waitStatus;
        if (ws == Node.SIGNAL)
            /*
             * This node has already set status asking a release
             * to signal it, so it can safely park.
             */
            return true;
        if (ws > 0) {
            /*
             * Predecessor was cancelled. Skip over predecessors and
             * indicate retry.
             */
            do {
                node.prev = pred = pred.prev;
            } while (pred.waitStatus > 0);
            pred.next = node;
        } else {
            /*
             * waitStatus must be 0 or PROPAGATE.  Indicate that we
             * need a signal, but don't park yet.  Caller will need to
             * retry to make sure it cannot acquire before parking.
             */
            pred.compareAndSetWaitStatus(ws, Node.SIGNAL);
        }
        return false;
    }

    /**
     * 获取本地JVM锁，head 节点直接进入等待状态，
     * 非 head 节点初始化 fifo 队列后进入等待状态
     *
     * @param waitTime 等待时间
     * @return true:分布式锁已被释放，尝试抢占 false:等待超时
     */
    public final boolean acquire(long waitTime) {
        if (tryAcquire())
            return waitingForWakeup(waitTime, null);
        else
            return joinTheTeam(waitTime);
    }

    /**
     * 等待唤醒
     *
     * @param waitTime 等待时间
     * @param node     当前线程所代表的节点
     * @return true:被唤醒 false:正常苏醒
     */
    private boolean waitingForWakeup(long waitTime, Node node) {
        final Thread current = Thread.currentThread();
        long l = TimeUnit.MILLISECONDS.toNanos(waitTime);
        waitTime = System.currentTimeMillis() + waitTime;
        LockSupport.parkNanos(current, l);
        // 等待时间小于当前时间
        if (waitTime < System.currentTimeMillis()) {
            // 持有锁
            if (getExclusiveOwnerThread() == current) {
                // fifo 队列已初始化
                if (hasQueuedPredecessors()) {
                    release();
                } else {
                    // fifo 对列未初始化
                    setExclusiveOwnerThread(null);
                }
            } else {
                // 非 head 节点
                cancelAcquire(node);
            }
            return false;
        }
        return true;
    }

    /**
     * 尝试获取前置锁
     *
     * @return true 成功 false 失败
     */
    @ReservedStackAccess
    public synchronized boolean tryAcquire() {
        final Thread current = Thread.currentThread();
        if (null == getExclusiveOwnerThread()) {
            setExclusiveOwnerThread(current);
            return true;
        }
        return current == getExclusiveOwnerThread();
    }

    /**
     * 本地 JVM 锁已被抢占，head 后面的一个节点负责初始化 fifo 对列，
     * 并进入睡眠状态，以等待锁施放事件器的唤醒
     *
     * @param waitTime 等待时间
     * @return true:分布式锁已被释放，尝试抢占 false:等待超时
     */
    public boolean joinTheTeam(long waitTime) {
        Node node = addWaiter(Node.EXCLUSIVE);
        try {
            for (; ; ) {
                if (shouldParkAfterFailedAcquire(node.prev, node))
                    return waitingForWakeup(waitTime, node);
            }
        } catch (Exception t) {
            cancelAcquire(node);
        }
        return false;
    }

    /**
     * 判断对列是否初始化
     *
     * @return true:初始化 false:未初始化
     */
    public final boolean hasQueuedPredecessors() {
        Node h, s;
        if ((h = head) != null) {
            if ((s = h.next) == null || s.waitStatus > 0) {
                s = null; // traverse in case of concurrent cancellation
                for (Node p = tail; p != h && p != null; p = p.prev) {
                    if (p.waitStatus <= 0)
                        s = p;
                }
            }
            if (s != null && s.thread != Thread.currentThread())
                return true;
        }
        return false;
    }

    /**
     * 唤醒 head 节点，使其能够尝试获取分布式锁
     *
     * @return true:唤醒成功
     */
    public final boolean doSignal() {
        if (head != null)
            LockSupport.unpark(getExclusiveOwnerThread());
        return true;
    }

    /**
     * 释放 JVM 本地锁
     */
    @ReservedStackAccess
    public final void release() {
        if (Thread.currentThread() != getExclusiveOwnerThread())
            throw new IllegalMonitorStateException();
        // 队列尚未初始化
        if (!hasQueuedPredecessors()) {
            setExclusiveOwnerThread(null);
            return;
        }
        Node h = head;
        Node s = h.next != null ? h.next : tailIteration(h);
        if (s != null) {
            setHead(s);
        } else {
            setExclusiveOwnerThread(null);
        }
        h.next = null;
    }

    /**
     * 为当前线程和给定模式创建节点并使其入队
     *
     * @param mode Node.EXCLUSIVE表示独占，Node.SHARED表示共享
     * @return 新节点
     */
    private Node addWaiter(Node mode) {
        Node node = new Node(mode);

        for (; ; ) {
            Node oldTail = tail;
            if (oldTail != null) {
                node.setPrevRelaxed(oldTail);
                if (compareAndSetTail(oldTail, node)) {
                    oldTail.next = node;
                    return node;
                }
            } else {
                initializeSyncQueue();
            }
        }
    }

    /**
     * 将队列的头设置为节点，从而出列。仅由获取方法调用。
     * 为了GC和抑制不必要的信号和遍历，还清空了未使用的字段
     *
     * @param node 节点
     */
    private void setHead(Node node) {
        head = node;
        setExclusiveOwnerThread(node.thread);
        node.thread = null;
        node.prev = null;
    }

    /**
     * 取消正在进行的尝试获取节点。head 节点是不能调用这个方法的，但是大量 node 同一时间失效包括 head 节点时，
     * 在取消的过程中 node 的角色会发生改变，导致正在取消的 node 身份变成 head，
     * 因此方法加上 synchronized 强制每次都读取最新的 head 身份，当身份是 head 时走特殊流程，这样写有点别扭，但是最简单干脆
     *
     * @param node 节点
     */
    private synchronized void cancelAcquire(Node node) {
        // Ignore if node doesn't exist
        if (node == null)
            return;
        // node 身份发生变化时，走特殊流程
        if (node == head || node.thread == getExclusiveOwnerThread()) {
            release();
            return;
        }

        node.thread = null;

        // Skip cancelled predecessors
        Node pred = node.prev;
        while (pred.waitStatus > 0)
            node.prev = pred = pred.prev;

        // predNext is the apparent node to unsplice. CASes below will
        // fail if not, in which case, we lost race vs another cancel
        // or signal, so no further action is necessary, although with
        // a possibility that a cancelled node may transiently remain
        // reachable.
        Node predNext = pred.next;

        // Can use unconditional write instead of CAS here.
        // After this atomic step, other Nodes can skip past us.
        // Before, we are free of interference from other threads.
        node.waitStatus = Node.CANCELLED;

        // If we are the tail, remove ourselves.
        if (node == tail && compareAndSetTail(node, pred)) {
            pred.compareAndSetNext(predNext, null);
        } else {
            // If successor needs signal, try to set pred's next-link
            // so it will get one. Otherwise wake it up to propagate.
            int ws;
            if (pred != head &&
                    ((ws = pred.waitStatus) == Node.SIGNAL ||
                            (ws <= 0 && pred.compareAndSetWaitStatus(ws, Node.SIGNAL))) &&
                    pred.thread != null) {
                Node next = node.next;
                if (next != null && next.waitStatus <= 0)
                    pred.compareAndSetNext(predNext, next);
            } else {
                unparkSuccessor(node);
            }

            node.next = node; // help GC
        }
    }

    /**
     * 节点需要被取消掉，将节点的前后关联关系进行切换，如果有需要的话
     *
     * @param node 节点
     */
    private void unparkSuccessor(Node node) {
        /*
         * If status is negative (i.e., possibly needing signal) try
         * to clear in anticipation of signalling.  It is OK if this
         * fails or if status is changed by waiting thread.
         */
        int ws = node.waitStatus;
        if (ws < 0)
            node.compareAndSetWaitStatus(ws, 0);

        /*
         * Thread to unpark is held in successor, which is normally
         * just the next node.  But if cancelled or apparently null,
         * traverse backwards from tail to find the actual
         * non-cancelled successor.
         */
        Node s = tailIteration(node);

        if (s != null && null != head) {
            node.prev.compareAndSetNext(node, s);
            s.prev = node.prev;
        }
    }

    /**
     * 获取节点下一个有效节点
     *
     * @param node 节点
     * @return 有效节点
     */
    private Node tailIteration(Node node) {
        Node s = node.next;
        if (s == null || s.waitStatus > 0) {
            s = null;
            for (Node p = tail; p != node && p != null; p = p.prev)
                if (p.waitStatus <= 0)
                    s = p;
        }
        return s;
    }

    /**
     * 在第一次争用时初始化 head 和 tail
     */
    private final void initializeSyncQueue() {
        Node h;
        if (HEAD.compareAndSet(this, null, (h = new Node())))
            tail = h;
    }

    /**
     * CAS tail
     */
    private final boolean compareAndSetTail(Node expect, Node update) {
        return TAIL.compareAndSet(this, expect, update);
    }

    static final class Node {

        /**
         * Marker to indicate a node is waiting in exclusive mode
         */
        static final Node EXCLUSIVE = null;

        /**
         * waitStatus value to indicate thread has cancelled.
         */
        static final int CANCELLED = 1;

        /**
         * waitStatus value to indicate successor's thread needs unparking.
         */
        static final int SIGNAL = -1;
        // VarHandle mechanics
        private static final VarHandle NEXT;
        private static final VarHandle PREV;
        private static final VarHandle THREAD;
        private static final VarHandle WAITSTATUS;

        static {
            try {
                MethodHandles.Lookup l = MethodHandles.lookup();
                NEXT = l.findVarHandle(Node.class, "next", Node.class);
                PREV = l.findVarHandle(Node.class, "prev", Node.class);
                THREAD = l.findVarHandle(Node.class, "thread", Thread.class);
                WAITSTATUS = l.findVarHandle(Node.class, "waitStatus", int.class);
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        /**
         * 初始为 0，队列初始化后，如果有后续节点，会被后续节点修改为-1
         */
        volatile int waitStatus;
        /**
         * Link to predecessor node that current node/thread relies on
         * for checking waitStatus. Assigned during enqueuing, and nulled
         * out (for sake of GC) only upon dequeuing.  Also, upon
         * cancellation of a predecessor, we short-circuit while
         * finding a non-cancelled one, which will always exist
         * because the head node is never cancelled: A node becomes
         * head only as a result of successful acquire. A
         * cancelled thread never succeeds in acquiring, and a thread only
         * cancels itself, not any other node.
         */
        volatile Node prev;
        /**
         * Link to the successor node that the current node/thread
         * unparks upon release. Assigned during enqueuing, adjusted
         * when bypassing cancelled predecessors, and nulled out (for
         * sake of GC) when dequeued.  The enq operation does not
         * assign next field of a predecessor until after attachment,
         * so seeing a null next field does not necessarily mean that
         * node is at end of queue. However, if a next field appears
         * to be null, we can scan prev's from the tail to
         * double-check.  The next field of cancelled nodes is set to
         * point to the node itself instead of null, to make life
         * easier for isOnSyncQueue.
         */
        volatile Node next;
        /**
         * The thread that enqueued this node.  Initialized on
         * construction and nulled out after use.
         */
        volatile Thread thread;
        /**
         * Link to next node waiting on condition, or the special
         * value SHARED.  Because condition queues are accessed only
         * when holding in exclusive mode, we just need a simple
         * linked queue to hold nodes while they are waiting on
         * conditions. They are then transferred to the queue to
         * re-acquire. And because conditions can only be exclusive,
         * we save a field by using special value to indicate shared
         * mode.
         */
        Node nextWaiter;

        /**
         * Establishes initial head or SHARED marker.
         */
        Node() {
        }

        /**
         * Constructor used by addWaiter.
         */
        Node(Node nextWaiter) {
            this.nextWaiter = nextWaiter;
            THREAD.set(this, Thread.currentThread());
        }

        /**
         * Constructor used by addConditionWaiter.
         */
        Node(int waitStatus) {
            WAITSTATUS.set(this, waitStatus);
            THREAD.set(this, Thread.currentThread());
        }

        /**
         * Returns previous node, or throws NullPointerException if null.
         * Use when predecessor cannot be null.  The null check could
         * be elided, but is present to help the VM.
         *
         * @return the predecessor of this node
         */
        final Node predecessor() {
            Node p = prev;
            if (p == null)
                throw new NullPointerException();
            else
                return p;
        }

        /**
         * CASes waitStatus field.
         */
        final boolean compareAndSetWaitStatus(int expect, int update) {
            return WAITSTATUS.compareAndSet(this, expect, update);
        }

        /**
         * CASes next field.
         */
        final boolean compareAndSetNext(Node expect, Node update) {
            return NEXT.compareAndSet(this, expect, update);
        }

        final void setPrevRelaxed(Node p) {
            PREV.set(this, p);
        }
    }

}

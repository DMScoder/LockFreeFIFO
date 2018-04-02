import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Created by Immortan on 4/1/2018.
 */
public class LockFreeFIFO {

    private static AtomicReferenceArray atomicReferenceArray;
    private static AtomicInteger head;
    private static AtomicInteger tail;
    private static int k, size;
    private Random r = new Random();


    public LockFreeFIFO(int size, int k){
        atomicReferenceArray = new AtomicReferenceArray(size * k);
        head = new AtomicInteger(0);
        tail = new AtomicInteger(0);
        this.k = k;
        this.size = size;
    }

    public boolean enqueue(Object item){
        while(true) {
            int tail_old = tail.get();
            int head_old = head.get();

            int index = find_empty_slot(tail_old);
            Object item_old = atomicReferenceArray.get(index);

            if (tail_old == tail.get()) {
                if (atomicReferenceArray.get(index) == null) {
                    if (atomicReferenceArray.compareAndSet(index, item_old, item)) {
                        if(committed(tail_old,item,index))
                            return true;
                    }
                }
                else{
                    if(tail_old + k == head_old){
                        if(atomicReferenceArray.get(head_old)!=null){
                            if(head_old==head.get())
                                return false;
                        }
                    }
                    else{
                        head.getAndAdd(k);
                    }
                    tail.getAndAdd(k);
                }
            }
        }
    }

    private boolean committed(int tail_old, Object item, int index){
        if(atomicReferenceArray.get(index) != item)
            return false;

        int head_current = head.get();
        int tail_current = tail.get();

        if(index < head_current && index > tail_current)
            return true;

        else if(index == tail_current){
            if(!atomicReferenceArray.compareAndSet(index, item, null))
                return true;
        }
        else{
            int head_new = head_current;
            if(head.compareAndSet(head_current,head_new))
                return true;
            if(!atomicReferenceArray.compareAndSet(index,item,null))
                return true;
        }

        return false;
    }

    private int find_empty_slot(int tail_old){
        int start = tail_old + r.nextInt(k);
        int index = start;
        do{
            if(atomicReferenceArray.get(index)==null)
                return index;

            if(index == tail_old + k - 1){
                index = tail_old;
            }
            else{
                index++;
            }
        }while(index!=start);

        return start;
    }

    private int find_item(int head_old){
        int start = head_old + r.nextInt(k);
        int index = start;
        do{
            if(atomicReferenceArray.get(index)!=null)
                return index;

            if(index == head_old + k - 1){
                index = head_old;
            }
            else{
                index++;
            }
        }while(index!=start);

        return start;
    }

    public Object dequeue(){
        while(true){
            int head_old = head.get();
            int index = find_item(head_old);
            Object item = atomicReferenceArray.get(index);
            int tail_old = tail.get();

            if(head_old == head.get()){
                if(item!=null){
                    if(head_old == tail_old){
                        tail.getAndAdd(k);
                    }

                    if(atomicReferenceArray.compareAndSet(index,item,null)){
                        return item;
                    }
                }
                else{
                    if(head_old == tail_old && tail_old == tail.get())
                        return null;
                    head.getAndAdd(k);
                }
            }
        }
    }

    public static void main(String args[]){
        LockFreeFIFO fifo = new LockFreeFIFO(5,10);

        for(int i=0;i<9;i++)
            fifo.enqueue(i);
        for(int i=0;i<9;i++)
            System.out.println(fifo.dequeue());
    }
}

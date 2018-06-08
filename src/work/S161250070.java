package work;

import bottom.BottomMonitor;
import bottom.BottomService;
import bottom.Task;
import main.Schedule;

import java.io.IOException;

/**
 *
 * 注意：请将此类名改为 S+你的学号   eg: S161250001
 * 提交时只用提交此类和说明文档
 *
 * 在实现过程中不得声明新的存储空间（不得使用new关键字，反射和java集合类）
 * 所有新声明的类变量必须为final类型
 * 不得创建新的辅助类
 *
 * 可以生成局部变量
 * 可以实现新的私有函数
 *
 * 可用接口说明:
 *
 * 获得当前的时间片
 * int getTimeTick()
 *
 * 获得cpu数目
 * int getCpuNumber()
 *
 * 对自由内存的读操作  offset 为索引偏移量， 返回位置为offset中存储的byte值
 * byte readFreeMemory(int offset)
 *
 * 对自由内存的写操作  offset 为索引偏移量， 将x写入位置为offset的内存中
 * void writeFreeMemory(int offset, byte x)
 *
 */
public class S161250070 extends Schedule{


    // 最新任务ID其实存放地址
    private static final int latestTaskBeginner = 0;
    // cpu状态起始地址
    private static final int cpuStateBeginner = latestTaskBeginner + 4;
    // 资源位示图起始地址
    private static final int resourceBeginner = cpuStateBeginner + 5 * 4;
    // pcb寻址表起始地址
    private static final int pcbBitBeginner = resourceBeginner + 128;
    // pcb存储空间起始地址
    private static final int pcbBeginner = pcbBitBeginner + 1000*4;

    private static final int PCB_tidBeginner = 0;
    private static final int PCB_arrivedTimeBeginner = 4;
    private static final int PCB_cpuTimeBeginner = 8;
    private static final int PCB_leftTimeBeginner = 12;
    private static final int PCB_rsLengthBeginner = 16;
    private static final int PCB_idleTimeBeginner = 20;
    private static final int PCB_resourceBeginner = 24;


    @Override
    public void ProcessSchedule(Task[] arrivedTask, int[] cpuOperate) {
        if(arrivedTask != null && arrivedTask.length != 0){
            for(Task task : arrivedTask){
                recordTask(task, getTimeTick());
            }
        }

        cleanAllResource();
        int cpuNumber = getCpuNumber()-1;
        int taskNumber = readInteger(latestTaskBeginner);

        while(cpuNumber >= 0 && taskNumber > 0) {
            int highest = 0;
            double weight = 0;
            double temp = 0;
            for (int i = 1; i <= taskNumber; i++) {
                if (isTaskFinish(i)) continue;
                if(!isUseResource(i)) continue;
                if((temp=(1.0*(getCpuTime(i) + getIdleTime(i))/getCpuTime(i))) >= weight){
                    if((weight != temp)) {
                        highest = i;
                        weight = temp;
                    }else if(Math.random()*2>1){
                        highest = i;
                        weight = temp;
                    }
                }
            }
            if(highest == 0)
                break;

            useResource(highest);
            cpuOperate[cpuNumber--] = highest;
            countDownLeft(highest);
        }
        for (int i = 1; i <= taskNumber; i++) {
            if (isTaskFinish(i)) continue;
            boolean flag = false;
            for(int j=0; j<=getCpuNumber()-1;j++){
                if(cpuOperate[j]==i){
                    flag = true;
                }
            }
            if(!flag){
                plusIdleTime(i);
            }
        }
    }


    /**
     * 向自由内存中 读一个int型整数
     * @param beginIndex
     * @return
     */
    private int readInteger(int beginIndex){
        int ans = 0;
        ans += (readFreeMemory(beginIndex)&0xff)<<24;
        ans += (readFreeMemory(beginIndex+1)&0xff)<<16;
        ans += (readFreeMemory(beginIndex+2)&0xff)<<8;
        ans += (readFreeMemory(beginIndex+3)&0xff);
        return ans;
    }



    /**
     * 向自由内存中写一个int型整数
     * @param beginIndex
     * @param value
     */
    private void writeInteger(int beginIndex, int value){
        writeFreeMemory(beginIndex+3, (byte) ((value&0x000000ff)));
        writeFreeMemory(beginIndex+2, (byte) ((value&0x0000ff00)>>8));
        writeFreeMemory(beginIndex+1, (byte) ((value&0x00ff0000)>>16));
        writeFreeMemory(beginIndex, (byte) ((value&0xff000000)>>24));
    }

    /**
     * 在自由内存的pcb中
     * @param task
     * @param arrivedTime
     */
    private void recordTask(Task task, int arrivedTime){
        int newIndex = getNewTaskBeginIndex();
        writeInteger(newIndex+PCB_tidBeginner, task.tid);
        writeInteger(newIndex+PCB_arrivedTimeBeginner, arrivedTime);
        writeInteger(newIndex+PCB_cpuTimeBeginner, task.cpuTime);
        writeInteger(newIndex+PCB_leftTimeBeginner, task.cpuTime);
        writeInteger(newIndex + PCB_idleTimeBeginner, 0);
        writeInteger(newIndex+PCB_rsLengthBeginner, task.resource.length);
        for(int i = 0 ; i < task.resource.length; i++) {
            writeFreeMemory(newIndex+PCB_resourceBeginner+i, (byte) task.resource[i]);
        }
        writeInteger(latestTaskBeginner, task.tid);
        writeInteger(pcbBitBeginner+task.tid*4, newIndex);
    }



    private void plusIdleTime(int taskID) {
        int time = readInteger(getTaskBeginIndex(taskID)+PCB_idleTimeBeginner);
        writeInteger(getTaskBeginIndex(taskID)+PCB_idleTimeBeginner, time + 1);
    }

    private int getCpuTime(int taskID) {
       return readInteger(getTaskBeginIndex(taskID)+PCB_cpuTimeBeginner);
    }


    /**
     * 获得这个任务需要的资源长度
     * @param taskID
     * @return
     */
    private int getTaskResourceLength(int taskID){
        return readInteger(getTaskBeginIndex(taskID)+PCB_rsLengthBeginner);
    }

    /**
     * 获得存储该任务pcb的内存地址
     * @param taskID
     * @return
     */
    private int getTaskBeginIndex(int taskID){
        return readInteger(pcbBitBeginner+taskID*4);
    }

    private int getIdleTime(int taskID){
        return readInteger(getTaskBeginIndex(taskID)+ PCB_idleTimeBeginner);
    }



    /**
     * 获得新到达任务 存放内存地址
     * @return
     */
    private int getNewTaskBeginIndex(){
        int latestTaskID = readInteger(latestTaskBeginner);
        if(latestTaskID == 0) return pcbBeginner;
        return getTaskBeginIndex(latestTaskID)+getTaskResourceLength(latestTaskID)+PCB_resourceBeginner;
    }



    public void printAllTask(){
        int last =  readInteger(latestTaskBeginner);
        for(int i = 1 ; i <= last ; i++){
            int beginIndex = getTaskBeginIndex(i);
            System.out.println("Task ID: "+readInteger(beginIndex+PCB_tidBeginner));
            System.out.println("Task arrivedTime: "+readInteger(beginIndex+PCB_arrivedTimeBeginner));
            System.out.println("Task cpuTime: "+readInteger(beginIndex+PCB_cpuTimeBeginner));
            System.out.println("Task leftTime: "+readInteger(beginIndex+PCB_leftTimeBeginner));
            System.out.println("Task ResourceLength: "+readInteger(beginIndex+PCB_rsLengthBeginner));
            System.out.print("Task resource: ");
            int length = readInteger(beginIndex+PCB_rsLengthBeginner);
            for(int j = 0 ; j < length ; j++){
                System.out.print(readFreeMemory(beginIndex+PCB_resourceBeginner+j)+", ");
            }
            System.out.println();
            System.out.println();
        }
    }

    /**
     * 查看资源是否可用
     * @param taskID
     * @return
     */
    private boolean isUseResource(int taskID){
        int index = getTaskBeginIndex(taskID);
        int length = readInteger(index+PCB_rsLengthBeginner);

        for(int i = 0 ; i < length ; i++){
            byte temp = readFreeMemory(index+PCB_resourceBeginner+i);
            if(readFreeMemory(resourceBeginner+temp-1) != 0) return false;
        }

        return true;
    }

    private void useResource(int taskID) {
        int index = getTaskBeginIndex(taskID);
        int length = readInteger(index+PCB_rsLengthBeginner);

        for(int i = 0 ; i < length ; i++){
            byte temp = readFreeMemory(index+PCB_resourceBeginner+i);
            writeFreeMemory(resourceBeginner+temp-1, (byte) 1);
        }
    }

    /**
     * 记录剩余时间-1
     * @param taskID
     */
    private void countDownLeft(int taskID){
        int index = getTaskBeginIndex(taskID);
        int leftTime = readInteger(index+PCB_leftTimeBeginner);
        if(leftTime == 0) return;
        leftTime--;
        writeInteger(index+PCB_leftTimeBeginner, leftTime);
    }

    /**c
     * 判断任务是否执行完毕
     * @param taskID
     * @return
     */
    private boolean isTaskFinish(int taskID){
        int index = getTaskBeginIndex(taskID);
        int leftTime = readInteger(index+PCB_leftTimeBeginner);
        return leftTime == 0;
    }

    /**
     * 将所有资源设为可用
     */
    private void cleanAllResource(){
        for(int i = 0 ; i < 128 ; i++){
            writeFreeMemory(resourceBeginner+i, (byte) 0);
        }
    }



    /**
     * 执行主函数 用于debug
     * 里面的内容可随意修改
     * 你可以在这里进行对自己的策略进行测试，如果不喜欢这种测试方式，可以直接删除main函数
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        // 定义cpu的数量
        int cpuNumber = 2;
        // 定义测试文件
        String filename = "src/testFile/rand_5.csv";

        BottomMonitor bottomMonitor = new BottomMonitor(filename,cpuNumber);
        BottomService bottomService = new BottomService(bottomMonitor);
        Schedule schedule =  new S161250070();
        schedule.setBottomService(bottomService);

        //外部调用实现类
        for(int i = 0 ; i < 800 ; i++){
            Task[] tasks = bottomMonitor.getTaskArrived();
            int[] cpuOperate = new int[cpuNumber];

            // 结果返回给cpuOperate
            schedule.ProcessSchedule(tasks,cpuOperate);

            try {
                bottomService.runCpu(cpuOperate);
            } catch (Exception e) {
                System.out.println("Fail: "+e.getMessage());
                e.printStackTrace();
                return;
            }
            bottomMonitor.increment();
        }

        //打印统计结果
        bottomMonitor.printStatistics();
        System.out.println();

        //打印任务队列
        bottomMonitor.printTaskArrayLog();
        System.out.println();

        //打印cpu日志
        bottomMonitor.printCpuLog();


        if(!bottomMonitor.isAllTaskFinish()){
            System.out.println(" Fail: At least one task has not been completed! ");
        }
    }

}

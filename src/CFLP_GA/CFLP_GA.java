import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class CFLP_GA {
    private Instance instance;
    public Solution bestSolution;
    private int population_size;
    private double variable_percent; //变异概率
    private int inheritance_number; //遗传次数

    private List<Solution> solutions;
    private List<Solution> sonSolutions;

    public static void main(String[] argv) {
        FileWriter fileWritter = null;
        try {
            File file = new File("CFLP_GA_result.csv");

            if (!file.exists()) {
                file.createNewFile();
            }

            fileWritter = new FileWriter(file.getName(), true);
            fileWritter.write("Instance,Time,Cost,FacilityState,CustomerState\n"); // 保存为csv格式
            for (int i = 1; i <= 71; i++) {
                CFLP_GA cflp_ga = new CFLP_GA("Instances/p" + i, 100, 0.3, 1000);
                long startTime = System.currentTimeMillis();
                cflp_ga.initialPopulations();
                cflp_ga.genetic();
                long endtime = System.currentTimeMillis();
                cflp_ga.instance.writeSolutionToFile(fileWritter, cflp_ga.bestSolution, i, (endtime - startTime)*1.0/1000);
                System.out.println("Instance:" + i + " , Best: " + cflp_ga.bestSolution.cost + " , time: " + (endtime - startTime)*1.0/1000 + "s");
            }
            fileWritter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public CFLP_GA(String filepath, int population_size, double variable_percent, int inheritance_number) {
        instance = new Instance(filepath);
        this.population_size = population_size;
        this.bestSolution = null;
        this.variable_percent = variable_percent;
        this.inheritance_number = inheritance_number;
    }

    public void genetic() {
        for (int i = 0; i < this.inheritance_number; i++) {
            this.oneGenetic(); //遗传一代
        }
    }

    private void oneGenetic() {
        this.sonSolutions = new ArrayList<>();
        this.hybridization(); //杂交
        this.mutation(); //变异一次
        this.updateEntity();  //选择
        this.chooseBest(); //择优
    }

    /***
     * 获取最优父代(用于精英保留战略)
     * @return
     */
    private Solution getBestParent() {
        if(this.bestSolution == null) {
            Solution bestParent = this.solutions.get(0).clone();
            for(Solution solution : solutions) {
                if(solution.cost < bestParent.cost) {
                    bestParent = solution.clone();
                }
            }
            return bestParent;
        } else {
            return this.bestSolution.clone();
        }
    }

    /***
     * 使用轮盘赌方法选择父母亲
     * @return
     */
    private Solution getParentSolution() {
        Random random = new Random();
        double selectPercent = random.nextDouble();
        double distributionPercent = 0.0;
        for(Solution solution : solutions) {
            distributionPercent += solution.reproducePercent;
            if(distributionPercent > selectPercent) {
                return solution;
            }
        }
        return null;
    }

    /***
     * 初始化种群
     */
    public void initialPopulations() {
        solutions = new ArrayList<>();
        Solution greedySolution = getGreedySolution();
        bestSolution = greedySolution;
        solutions.add(greedySolution);
        for (int i = 1; i < population_size; i++) {
            int[] customDemand = new int[instance.customerNum];
            double cost = -1;
            while (cost == -1) {
                for (int j = 0; j < instance.customerNum; j++) {
                    customDemand[j] = (int)(Math.random() * instance.factoryNum);
                }
                cost = instance.valid(customDemand);
            }
            solutions.add(new Solution(customDemand, cost));
        }
        this.setReproducePercent();
    }

    /***
     * 选择函数
     */
    public void updateEntity() {
        List<Solution> allSolutions = new ArrayList<>();
        allSolutions.addAll(this.sonSolutions);
        allSolutions.addAll(this.solutions);
        Collections.sort(allSolutions); //排序所有个体
        List<Solution> bestEntities = new ArrayList<>();
        for(int i = 0; i < this.population_size; ++i) {
            bestEntities.add(allSolutions.get(i).clone());
        }
        Collections.shuffle(bestEntities);
        solutions = new ArrayList<>();
        for(int i = 0; i < population_size; ++i) {
            solutions.add(bestEntities.get(i)); //选择
        }
        this.setReproducePercent(); //重新设置选择概率(归一)
    }

    private void setReproducePercent() {
        double sumLengthToOne = 0.0;
        for (Solution solution : this.solutions) {
            sumLengthToOne += 1/solution.cost;
        }

        for (Solution solution : this.solutions) {
            solution.reproducePercent = (1 / solution.cost) / sumLengthToOne;
        }
    }

    private void hybridization() {
        for(int i = 0; i < this.population_size; ++i) { //杂交
            this.oneHybridization();
        }
    }

    private void oneHybridization() {
        Solution father = this.getParentSolution(); //父亲
        Solution mother = this.getParentSolution(); //母亲
        Solution best = this.getBestParent(); //最优个体不参与交叉互换
        while(father == null || father.equals(best)) {
            father = this.getParentSolution();
        }
        int cnt = 0;
        while(mother == null || father.equals(mother) || mother.equals(best)) {
            if(cnt > this.population_size / 2) {
                break;
            }
            cnt++;
            mother = this.getParentSolution(); //直到父母不等
        }

        List<Solution> newSolutions = this.swap(father, mother);

        for(Solution solution : newSolutions) {
            if (solution != null && !isExist(solution, sonSolutions) && !isExist(solution, solutions)) { //去除重复个体
                this.sonSolutions.add(solution); //添加到儿子列表
            }
        }
    }

    private boolean isExist(Solution new_solution, List<Solution> solutions) {
        for (Solution solution : solutions) {
            if (solution.equals(new_solution))
                return true;
        }
        return false;
    }


    /***
     * 交叉函数
     */
    public List<Solution> swap(Solution father, Solution mother) {
        List<Solution> result = new ArrayList<>();
        int count = 0;
        while (count < 10 && result.size() < 2) {    // 若未生成两个valid子代，继续执行，最多运行10次
            int index1 = (int) (instance.customerNum * Math.random());
            int index2 = (int) (instance.customerNum * Math.random());
            while (index1 == index2) {
                index1 = (int) (instance.customerNum * Math.random());
                index2 = (int) (instance.customerNum * Math.random());
            }
            if (index1 > index2) {
                int tmp = index1;
                index1 = index2;
                index2 = tmp;
            }
            int[] fatherClone = father.customerDemand.clone();
            int[] motherClone = mother.customerDemand.clone();
            for (int i = index1; i <= index2; i++) {
                fatherClone[i] = mother.customerDemand[i];
                motherClone[i] = father.customerDemand[i];
            }
            double fatherCost = instance.valid(fatherClone);
            double motherCost = instance.valid(motherClone);
            if (fatherCost != -1) {
                result.add(new Solution(fatherClone, fatherCost));
            }
            if (motherCost != -1) {
                result.add(new Solution(motherClone, motherCost));
            }
            ++count;
        }
        return result;
    }

    /***
     * 根据变异率对子代调用变异函数
     */
    public void mutation() {
        for (Solution solution : sonSolutions) {
            double percent = Math.random();
            if (percent < this.variable_percent) {
                oneVariable(solution);
            }
        }
    }

    /***
     * 变异函数，随机分配一个顾客的工厂
     */
    public void oneVariable(Solution solution) {
        int index = (int) (instance.customerNum * Math.random());
        int factoryIndex = (int) (instance.factoryNum * Math.random());
        solution.customerDemand[index] = factoryIndex;
    }


    /***
     * 找出当前最优解
     */
    private void chooseBest() {
        for(Solution solution : solutions) {
            if(solution.cost < bestSolution.cost) {
                this.bestSolution =  solution.clone();
                this.bestSolution.reproducePercent = solution.reproducePercent;
            }
        }
    }

    /***
     * 使用贪心算法求的初始的解，加入到初始种群
     * @return
     */
    public Solution getGreedySolution() {
        List<Integer> openFactories = new ArrayList<>();
        List<Integer> customerList = new ArrayList<>();
        List<Integer> unOpenFactory = new ArrayList<>();
        for (int i = 0; i < instance.factoryNum; i++)
            unOpenFactory.add(i);
        // 初始化UpOpenFactory与openFactories，使两个List都有工厂，方便下一步进行贪心算法
        Instance.MinInfo[] minInfos = new Instance.MinInfo[instance.customerNum];
        Instance.MinInfo minInfo = instance.findMinCost();
        double fee = 0;
        if (instance.factories.get(minInfo.i).available(instance.customers.get(minInfo.j).need)) {
            openFactories.add(minInfo.i);
            unOpenFactory.remove(minInfo.i);
            customerList.add(minInfo.j);
            minInfos[minInfo.j] = minInfo;
            instance.factories.get(minInfo.i).getDemend(instance.customers.get(minInfo.j).need);
            fee += (instance.factories.get(minInfo.i).open_cost + minInfo.cost);
            instance.unavailableCustomer(minInfo.j);
        }
        Instance.MinInfo minInfo1 = null;
        while (customerList.size() < instance.customerNum) {
            if (unOpenFactory.size() > 0) {
                minInfo1 = instance.findMinCostOfList(unOpenFactory);
            } // 从未开启工厂中选取开销最小的
            Instance.MinInfo minInfo2 = instance.findMinCostOfList(openFactories); // 从已开启工厂中选取开销最小的
            if (unOpenFactory.size() > 0
                    && instance.factories.get(minInfo1.i).available(instance.customers.get(minInfo1.j).need)
                    && (minInfo2.cost != Double.MAX_VALUE
                    && instance.factories.get(minInfo1.i).open_cost + minInfo1.cost < minInfo2.cost // 比较总开销(未开启工厂的加上开启工厂的开销)
                    || minInfo2.cost == Double.MAX_VALUE)) {
                openFactories.add(minInfo1.i);
                instance.factories.get(minInfo1.i).getDemend(instance.customers.get(minInfo1.j).need);
                fee += instance.factories.get(minInfo1.i).open_cost + minInfo1.cost;
                customerList.add(minInfo1.j);
                minInfos[minInfo1.j] = minInfo1;
                unOpenFactory.remove((Integer) minInfo1.i);
                instance.unavailableCustomer(minInfo1.j);
            } else if (instance.factories.get(minInfo2.i).available(instance.customers.get(minInfo2.j).need)) { // 选择已开启工厂开销更少
                customerList.add(minInfo2.j);
                minInfos[minInfo2.j] = minInfo2;
                instance.factories.get(minInfo2.i).getDemend(instance.customers.get(minInfo2.j).need);
                fee += minInfo2.cost;
                instance.unavailableCustomer(minInfo2.j);
            } else if (!instance.factories.get(minInfo2.i).available(instance.customers.get(minInfo2.j).need)) // 容量分配满则使其不可用
                instance.unavailableFactory(minInfo2.i);
        }
        int[] customerDemand = new int[customerList.size()];
        for (Instance.MinInfo minInfo2 : minInfos) {
            customerDemand[minInfo2.j] = minInfo2.i;
        }
        return new Solution(customerDemand, fee);
    }
}

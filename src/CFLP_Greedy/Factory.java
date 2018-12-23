public class Factory {
    public int capacity;
    public int used;
    public int open_cost;

    public Factory() {
        capacity = 0;
        used = 0;
        open_cost = Integer.MAX_VALUE;
    }

    public Factory(int capacity, int used, int open_cost) {
        this.capacity = capacity;
        this.used = used;
        this.open_cost = open_cost;
    }


    public boolean available(double demand) {
        if (demand + used >= capacity)
            return false;
        return true;
    }

    public void getDemend(double demand) {
        used += demand;
    }
}

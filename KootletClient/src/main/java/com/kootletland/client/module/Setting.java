package com.kootletland.client.module;

public abstract class Setting {
    public final String name;
    public final String key;

    protected Setting(String name) {
        this.name = name;
        this.key = name.replace(" ", "").toLowerCase();
    }

    public abstract String save();
    public abstract void load(String s);

    public static class Bool extends Setting {
        public boolean value;
        public float anim;

        public Bool(String name, boolean def) {
            super(name);
            value = def;
            anim = def ? 1F : 0F;
        }
        @Override public String save() { return String.valueOf(value); }
        @Override public void load(String s) { value = Boolean.parseBoolean(s.trim()); anim = value ? 1F : 0F; }
    }

    public static class Num extends Setting {
        public double value;
        public final double min, max, step;

        public Num(String name, double def, double min, double max, double step) {
            super(name);
            this.min = min;
            this.max = max;
            this.step = step;
            set(def);
        }
        public void set(double v) {
            v = Math.max(min, Math.min(max, v));
            v = Math.round(v / step) * step;
            value = Math.round(v * 1000.0) / 1000.0;
        }
        public float f() { return (float) value; }
        public int i() { return (int) Math.round(value); }
        public String display() {
            return step >= 1.0 ? String.valueOf((int) value) : String.valueOf(Math.round(value * 10.0) / 10.0);
        }
        @Override public String save() { return String.valueOf(value); }
        @Override public void load(String s) {
            try { set(Double.parseDouble(s.trim())); } catch (NumberFormatException ignored) {}
        }
    }

    public static class Mode extends Setting {
        public final String[] modes;
        public int index;

        public Mode(String name, String... modes) {
            super(name);
            this.modes = modes;
        }
        public String get() { return modes[index]; }
        public void cycle() { index = (index + 1) % modes.length; }
        @Override public String save() { return get(); }
        @Override public void load(String s) {
            for (int i = 0; i < modes.length; i++) {
                if (modes[i].equalsIgnoreCase(s.trim())) index = i;
            }
        }
    }
}

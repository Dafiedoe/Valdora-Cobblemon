package net.valdora.battle;

public enum LevelMode {
    NORMAL(),
    RELATIVE(),
    FLAT();

    LevelMode() {

    }

    public static class Factory implements SimpleFactory<LevelMode> {
        private final String levelMode;

        public Factory(String levelMode) {
            this.levelMode = levelMode;
        }

        @Override
        public LevelMode create() {
            return switch (levelMode) {
                case "normal" -> LevelMode.NORMAL;
                case "relative" -> LevelMode.RELATIVE;
                case "flat" -> LevelMode.FLAT;
                default -> LevelMode.NORMAL;
            };
        }
    }
}
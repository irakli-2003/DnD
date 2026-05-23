package com.dnd.model.magic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Concentration {
    private DifficultyLevel difficultyLevel;
    private boolean preventsMovement;
    private int requiredRoll;

    public Concentration() {
    }

    public Concentration(DifficultyLevel difficultyLevel, boolean preventsMovement) {
        setDifficultyLevel(difficultyLevel);
        this.preventsMovement = preventsMovement;
    }

    public DifficultyLevel getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(DifficultyLevel difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
        this.requiredRoll = difficultyLevel == null ? 0 : difficultyLevel.getRequiredRoll();
    }

    public boolean isPreventsMovement() {
        return preventsMovement;
    }

    public void setPreventsMovement(boolean preventsMovement) {
        this.preventsMovement = preventsMovement;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public int getRequiredRoll() {
        return requiredRoll;
    }

    public boolean keepConcentration(int rolled) {
        return rolled >= requiredRoll;
    }

    public enum DifficultyLevel {
        EASY(5),
        MEDIUM(10),
        HARD(15);

        private final int requiredRoll;

        DifficultyLevel(int requiredRoll) {
            this.requiredRoll = requiredRoll;
        }

        @JsonCreator
        public static DifficultyLevel fromJson(String value) {
            if (value == null) {
                return null;
            }
            return DifficultyLevel.valueOf(value.trim().toUpperCase());
        }

        public int getRequiredRoll() {
            return requiredRoll;
        }
    }
}


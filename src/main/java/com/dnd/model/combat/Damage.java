package com.dnd.model.combat;

import com.dnd.model.interfaces.Printable;
import com.dnd.model.item.armors.Armor;
import com.dnd.model.item.armors.ArmorMaterial;
import com.dnd.model.world.Dice;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Damage implements Printable {
    private Dice amount;
    private String typeId;

    public Damage() {
    }

    public Damage(Dice amount) {
        this.amount = amount;
    }

    public Damage(Dice amount, String typeId) {
        this.amount = amount;
        this.typeId = typeId;
    }

    public DamageResolution resolveAgainst(Armor armor, int attackRoll) {
        ArmorMaterial material = armor == null ? ArmorMaterial.NONE : armor.getMaterial();
        String normalizedType = typeId == null ? "" : typeId.trim().toLowerCase(Locale.ROOT);

        double targetMultiplier = 1.0;
        double armorMultiplier = 0.0;
        List<String> triggeredEffects = new ArrayList<>();

        switch (normalizedType) {
            case "slashing":
                if (material == ArmorMaterial.LEATHER) {
                    targetMultiplier = 0.5;
                    armorMultiplier = 1.0;
                } else if (material == ArmorMaterial.CHAIN) {
                    targetMultiplier = 0.0;
                    armorMultiplier = 1.0 / 3.0;
                } else if (material == ArmorMaterial.STEEL) {
                    targetMultiplier = 0.0;
                    armorMultiplier = 0.0;
                }
                break;
            case "cutting":
                if (material == ArmorMaterial.LEATHER) {
                    targetMultiplier = 2.0 / 3.0;
                    armorMultiplier = 1.0;
                } else if (material == ArmorMaterial.CHAIN) {
                    targetMultiplier = 1.0 / 3.0;
                    armorMultiplier = 2.0 / 3.0;
                } else if (material == ArmorMaterial.STEEL) {
                    targetMultiplier = 0.0;
                    armorMultiplier = 1.0 / 3.0;
                }
                break;
            case "bludgeoning":
                if (material == ArmorMaterial.CHAIN) {
                    targetMultiplier = 2.0 / 3.0;
                    armorMultiplier = 2.0 / 3.0;
                } else if (material == ArmorMaterial.STEEL) {
                    targetMultiplier = 1.0 / 3.0;
                    armorMultiplier = 2.0 / 3.0;
                }
                break;
            case "piercing":
                if (material == ArmorMaterial.LEATHER) {
                    targetMultiplier = 3.0 / 4.0;
                    armorMultiplier = 1.0;
                } else if (material == ArmorMaterial.CHAIN) {
                    targetMultiplier = 0.5;
                    armorMultiplier = 2.0 / 3.0;
                } else if (material == ArmorMaterial.STEEL) {
                    targetMultiplier = 1.0 / 3.0;
                    armorMultiplier = 1.0 / 3.0;
                }
                break;
            case "fire":
                if (material == ArmorMaterial.LEATHER) {
                    targetMultiplier = 2.0 / 3.0;
                    armorMultiplier = 1.0;
                } else if (material == ArmorMaterial.CHAIN) {
                    targetMultiplier = 2.0 / 3.0;
                    armorMultiplier = 1.0 / 3.0;
                    if (attackRoll > 15) {
                        triggeredEffects.add("heated_armor");
                    }
                } else if (material == ArmorMaterial.STEEL) {
                    targetMultiplier = 0.5;
                    armorMultiplier = 1.0 / 3.0;
                    if (attackRoll > 15) {
                        triggeredEffects.add("heated_armor");
                    }
                }
                break;
            case "cold":
                if (material == ArmorMaterial.LEATHER) {
                    targetMultiplier = 1.0 / 3.0;
                    armorMultiplier = 0.5;
                } else if (material == ArmorMaterial.CHAIN) {
                    targetMultiplier = 2.0 / 3.0;
                    armorMultiplier = 1.0 / 3.0;
                } else if (material == ArmorMaterial.STEEL) {
                    targetMultiplier = 0.5;
                    armorMultiplier = 0.5;
                }
                if (attackRoll > 15) {
                    triggeredEffects.add("freeze");
                }
                break;
            case "lightning":
                if (material == ArmorMaterial.LEATHER) {
                    targetMultiplier = 1.0;
                    armorMultiplier = 1.0;
                } else if (material == ArmorMaterial.CHAIN) {
                    targetMultiplier = 1.5;
                    armorMultiplier = attackRoll > 15 ? 0.5 : 0.0;
                } else if (material == ArmorMaterial.STEEL) {
                    targetMultiplier = 1.5;
                    armorMultiplier = attackRoll > 15 ? 0.5 : 0.0;
                }
                if (attackRoll > 15) {
                    triggeredEffects.add("shock");
                }
                break;
            case "acid":
                if (attackRoll > 15) {
                    targetMultiplier = 1.0;
                    armorMultiplier = 0.0;
                    triggeredEffects.add("burn");
                } else if (material == ArmorMaterial.LEATHER) {
                    targetMultiplier = 0.0;
                    armorMultiplier = 1.0;
                } else if (material == ArmorMaterial.CHAIN) {
                    targetMultiplier = 1.0;
                    armorMultiplier = 0.5;
                } else if (material == ArmorMaterial.STEEL) {
                    targetMultiplier = 0.0;
                    armorMultiplier = 0.5;
                }
                break;
            case "psychic":
                if (attackRoll > 15) {
                    triggeredEffects.add("fear");
                }
                break;
            case "bio":
                if (attackRoll > 15) {
                    triggeredEffects.add("infection");
                }
                break;
            default:
                break;
        }

        int baseAmount = getAmountValue();
        int damageToTarget = (int) Math.round(baseAmount * targetMultiplier);
        int damageToArmor = (int) Math.round(baseAmount * armorMultiplier);
        return new DamageResolution(damageToTarget, damageToArmor, triggeredEffects);
    }

    public Dice getAmount() {
        return amount;
    }

    public void setAmount(Dice amount) {
        this.amount = amount;
    }

    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    @Override
    public String toString() {
        return (amount != null ? amount.toString() : "0") + " " + (typeId != null ? typeId : "damage");
    }

    private int getAmountValue() {
        return amount == null ? 0 : amount.getSides();
    }

    public static class DamageResolution {
        private int damageToTarget;
        private int damageToArmor;
        private List<String> triggeredEffects;

        public DamageResolution() {
        }

        public DamageResolution(int damageToTarget, int damageToArmor, List<String> triggeredEffects) {
            this.damageToTarget = damageToTarget;
            this.damageToArmor = damageToArmor;
            this.triggeredEffects = triggeredEffects;
        }

        public int getDamageToTarget() {
            return damageToTarget;
        }

        public void setDamageToTarget(int damageToTarget) {
            this.damageToTarget = damageToTarget;
        }

        public int getDamageToArmor() {
            return damageToArmor;
        }

        public void setDamageToArmor(int damageToArmor) {
            this.damageToArmor = damageToArmor;
        }

        public List<String> getTriggeredEffects() {
            return triggeredEffects;
        }

        public void setTriggeredEffects(List<String> triggeredEffects) {
            this.triggeredEffects = triggeredEffects;
        }
    }
}


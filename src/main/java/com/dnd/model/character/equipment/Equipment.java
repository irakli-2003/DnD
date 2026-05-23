package com.dnd.model.character.equipment;

import com.dnd.model.item.BodyArmor;
import com.dnd.model.item.HandArmor;
import com.dnd.model.item.HeadArmor;
import com.dnd.model.item.Item;
import com.dnd.model.item.LegArmor;

import java.util.List;

public class Equipment {
    private HeadArmor headArmor;
    private BodyArmor bodyArmor;
    private LegArmor legArmor;
    private HandArmor handArmor;
    private Item rightHandItem;
    private Item leftHandItem;
    private List<Item> battleReadyItems;
    private List<Item> storedItems;

    public Equipment() {
    }

    public Equipment(HeadArmor headArmor, BodyArmor bodyArmor, LegArmor legArmor, HandArmor handArmor,
                     Item rightHandItem, Item leftHandItem, List<Item> battleReadyItems, List<Item> storedItems) {
        this.headArmor = headArmor;
        this.bodyArmor = bodyArmor;
        this.legArmor = legArmor;
        this.handArmor = handArmor;
        this.rightHandItem = rightHandItem;
        this.leftHandItem = leftHandItem;
        this.battleReadyItems = battleReadyItems;
        this.storedItems = storedItems;
    }

    public HeadArmor getHeadArmor() {
        return headArmor;
    }

    public void setHeadArmor(HeadArmor headArmor) {
        this.headArmor = headArmor;
    }

    public BodyArmor getBodyArmor() {
        return bodyArmor;
    }

    public void setBodyArmor(BodyArmor bodyArmor) {
        this.bodyArmor = bodyArmor;
    }

    public LegArmor getLegArmor() {
        return legArmor;
    }

    public void setLegArmor(LegArmor legArmor) {
        this.legArmor = legArmor;
    }

    public HandArmor getHandArmor() {
        return handArmor;
    }

    public void setHandArmor(HandArmor handArmor) {
        this.handArmor = handArmor;
    }

    public Item getRightHandItem() {
        return rightHandItem;
    }

    public void setRightHandItem(Item rightHandItem) {
        this.rightHandItem = rightHandItem;
    }

    public Item getLeftHandItem() {
        return leftHandItem;
    }

    public void setLeftHandItem(Item leftHandItem) {
        this.leftHandItem = leftHandItem;
    }

    public List<Item> getBattleReadyItems() {
        return battleReadyItems;
    }

    public void setBattleReadyItems(List<Item> battleReadyItems) {
        this.battleReadyItems = battleReadyItems;
    }

    public List<Item> getStoredItems() {
        return storedItems;
    }

    public void setStoredItems(List<Item> storedItems) {
        this.storedItems = storedItems;
    }
}



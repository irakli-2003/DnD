package com.dnd.data.dto;

import com.dnd.model.alchemy.AlchemyIngredient;

import java.util.List;

public class AlchemyIngredientCatalog {
    private List<AlchemyIngredient> ingredients;

    public AlchemyIngredientCatalog() {
    }

    public List<AlchemyIngredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<AlchemyIngredient> ingredients) {
        this.ingredients = ingredients;
    }
}


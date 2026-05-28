CREATE INDEX IF NOT EXISTS idx_item_outfit_id
    ON tb_item(outfit_id);

CREATE INDEX IF NOT EXISTS idx_outfit_colors_outfit_id
    ON tb_outfit_colors(outfit_id);

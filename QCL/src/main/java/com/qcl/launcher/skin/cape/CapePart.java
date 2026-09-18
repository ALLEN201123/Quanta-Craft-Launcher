package com.qcl.launcher.skin.cape;

/* loaded from: classes2.dex */
public enum CapePart {
    CAPE(0, "Cape", new CapePartSection[]{CapeSkin.FRONT.getCapePartSection(), CapeSkin.RIGHT.getCapePartSection(), CapeSkin.BACK.getCapePartSection(), CapeSkin.LEFT.getCapePartSection(), CapeSkin.TOP.getCapePartSection(), CapeSkin.BOTTOM.getCapePartSection()});

    private CapePartSection[] capeParts;
    private String displayName;
    private int id;

    CapePart(int i, String str, CapePartSection[] capePartSectionArr) {
        this.id = i;
        this.displayName = str;
        this.capeParts = capePartSectionArr;
    }

    CapePartSection getCapePartSection(int i) {
        return this.capeParts[i];
    }

    String getDisplayName() {
        return this.displayName;
    }

    int getPartId() {
        return this.id;
    }

    /* loaded from: classes2.dex */
    public enum CapeSkin {
        BACK("BACK", 2, 2, "Back", 1, 1, 10, 16),
        BOTTOM("BOTTOM", 5, 5, "Bottom", 11, 0, 10, 1),
        FRONT("FRONT", 0, 0, "Front", 12, 1, 10, 16),
        LEFT("LEFT", 3, 3, "Left", 0, 1, 1, 16),
        RIGHT("RIGHT", 1, 1, "Right", 11, 1, 1, 16),
        TOP("TOP", 4, 4, "Top", 1, 0, 10, 1);

        private String displayName;
        private int height;
        private int id;
        private int startX;
        private int startY;
        private int width;

        CapeSkin(String str, int i, int i2, String str2, int i3, int i4, int i5, int i6) {
            this.id = i2;
            this.width = i5;
            this.height = i6;
            this.displayName = str2;
            this.startX = i3;
            this.startY = i4;
        }

        CapePartSection getCapePartSection() {
            return new CapePartSection("Cape", this.displayName, this.startX, this.startY, this.width, this.height);
        }

        String getDisplayName() {
            return this.displayName;
        }

        int getHeight() {
            return this.height;
        }

        int getPartId() {
            return this.id;
        }

        int getStartX() {
            return this.startX;
        }

        int getStartY() {
            return this.startY;
        }

        int getWidth() {
            return this.width;
        }
    }
}

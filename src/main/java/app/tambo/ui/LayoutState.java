package app.tambo.ui;

record LayoutState(int servicesWidthPercent, int overviewHeightPercent) {
    private static final int MIN_SERVICES_WIDTH = 20;
    private static final int MAX_SERVICES_WIDTH = 45;
    private static final int MIN_OVERVIEW_HEIGHT = 30;
    private static final int MAX_OVERVIEW_HEIGHT = 65;
    private static final int STEP = 5;

    LayoutState {
        if (servicesWidthPercent < MIN_SERVICES_WIDTH || servicesWidthPercent > MAX_SERVICES_WIDTH) {
            throw new IllegalArgumentException("services width is outside its allowed range");
        }
        if (overviewHeightPercent < MIN_OVERVIEW_HEIGHT || overviewHeightPercent > MAX_OVERVIEW_HEIGHT) {
            throw new IllegalArgumentException("overview height is outside its allowed range");
        }
    }

    static LayoutState defaults() {
        return new LayoutState(30, 45);
    }

    LayoutState widerServices() {
        return new LayoutState(
                Math.min(MAX_SERVICES_WIDTH, servicesWidthPercent + STEP),
                overviewHeightPercent
        );
    }

    LayoutState narrowerServices() {
        return new LayoutState(
                Math.max(MIN_SERVICES_WIDTH, servicesWidthPercent - STEP),
                overviewHeightPercent
        );
    }

    LayoutState changeServicesWidth(int deltaPercent) {
        return new LayoutState(
                Math.max(MIN_SERVICES_WIDTH,
                        Math.min(MAX_SERVICES_WIDTH, servicesWidthPercent + deltaPercent)),
                overviewHeightPercent
        );
    }

    LayoutState tallerOverview() {
        return new LayoutState(
                servicesWidthPercent,
                Math.min(MAX_OVERVIEW_HEIGHT, overviewHeightPercent + STEP)
        );
    }

    LayoutState shorterOverview() {
        return new LayoutState(
                servicesWidthPercent,
                Math.max(MIN_OVERVIEW_HEIGHT, overviewHeightPercent - STEP)
        );
    }
}

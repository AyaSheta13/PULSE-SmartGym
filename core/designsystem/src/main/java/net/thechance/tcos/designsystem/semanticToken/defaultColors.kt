package net.thechance.tcos.designsystem.semanticToken

import net.thechance.tcos.designsystem.baseToken.ColorPalette

val defaultLightColors: ColorScheme = ColorScheme(
    primary = ColorPalette.sky.t800,
    primaryVariant = ColorPalette.sky.t50,
    secondary = ColorPalette.coffee.t800,
    secondaryVariant = ColorPalette.coffee.t50,
    container = ColorPalette.white,
    background = ColorPalette.gray.t100,
    onPrimary = ColorPalette.gray.t50,
    shadePrimary = ColorPalette.gray.t800,
    shadeSecondary = ColorPalette.gray.t600,
    shadeTertiary = ColorPalette.gray.t500,
    shadeQuaternary = ColorPalette.gray.t300,
    disable = ColorPalette.gray.t400,
    onDisable = ColorPalette.gray.t600,
    error = ColorPalette.cherry.t600,
    errorVariant = ColorPalette.cherry.t50,
    success = ColorPalette.lime.t600,
    successVariant = ColorPalette.lime.t50,
    warning = ColorPalette.honey.t500,
    warningVariant = ColorPalette.honey.t50
)

val defaultDarkColors: ColorScheme = ColorScheme(
    primary = ColorPalette.sky.t500,
    primaryVariant = ColorPalette.sky.t900,
    secondary = ColorPalette.coffee.t500,
    secondaryVariant = ColorPalette.coffee.t900,
    container = ColorPalette.gray.t800,
    background = ColorPalette.gray.t900,
    onPrimary = ColorPalette.gray.t50,
    shadePrimary = ColorPalette.gray.t100,
    shadeSecondary = ColorPalette.gray.t400,
    shadeTertiary = ColorPalette.gray.t500,
    shadeQuaternary = ColorPalette.gray.t700,
    disable = ColorPalette.gray.t600,
    onDisable = ColorPalette.gray.t700,
    error = ColorPalette.cherry.t400,
    errorVariant = ColorPalette.cherry.t900,
    success = ColorPalette.lime.t400,
    successVariant = ColorPalette.lime.t900,
    warning = ColorPalette.honey.t400,
    warningVariant = ColorPalette.honey.t800
)
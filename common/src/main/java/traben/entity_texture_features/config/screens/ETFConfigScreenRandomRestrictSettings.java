package traben.entity_texture_features.config.screens;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.text.Text;
import traben.entity_texture_features.ETFClientCommon;
import traben.entity_texture_features.ETFVersionDifferenceHandler;
import traben.entity_texture_features.config.ETFConfig;

import java.util.Objects;

//inspired by puzzles custom gui code
public class ETFConfigScreenRandomRestrictSettings extends ETFConfigScreen {
    protected ETFConfigScreenRandomRestrictSettings(Screen parent) {
        super(ETFVersionDifferenceHandler.getTextFromTranslation("config." + ETFClientCommon.MOD_ID + ".restrict_update_properties"), parent);

    }


    @Override
    protected void init() {
        super.init();
        this.addDrawableChild(getETFButton((int) (this.width * 0.55), (int) (this.height * 0.9), (int) (this.width * 0.2), 20,
                ScreenTexts.BACK,
                (button) -> Objects.requireNonNull(client).setScreen(parent)));
        this.addDrawableChild(getETFButton((int) (this.width * 0.25), (int) (this.height * 0.9), (int) (this.width * 0.22), 20,
                ETFVersionDifferenceHandler.getTextFromTranslation("dataPack.validation.reset"),
                (button) -> {
                    //temporaryETFConfig = new ETFConfig();
                    ETFConfigScreenMain.temporaryETFConfig.restrictUpdateProperties = true;
                    ETFConfigScreenMain.temporaryETFConfig.restrictMoonPhase = true;
                    ETFConfigScreenMain.temporaryETFConfig.restrictDayTime = true;
                    ETFConfigScreenMain.temporaryETFConfig.restrictWeather = true;
                    ETFConfigScreenMain.temporaryETFConfig.restrictBiome = true;
                    ETFConfigScreenMain.temporaryETFConfig.restrictBlock = true;
                    ETFConfigScreenMain.temporaryETFConfig.restrictHeight = true;
                    Objects.requireNonNull(client).setScreen(new ETFConfigScreenRandomRestrictSettings(parent));
                    this.onClose();
                    //Objects.requireNonNull(client).setScreen(parent);
                }));



        ButtonWidget restrictBiome = getETFButton((int) (this.width * 0.275), (int) (this.height * 0.4), (int) (this.width * 0.2), 20,
                Text.of(ETFVersionDifferenceHandler.getTextFromTranslation(
                        "config." + ETFClientCommon.MOD_ID + ".restrict_biome.title"
                ).getString() + ": " + (ETFConfigScreenMain.temporaryETFConfig.restrictBiome ? ScreenTexts.ON : ScreenTexts.OFF).getString()),
                (button) -> {
                    ETFConfigScreenMain.temporaryETFConfig.restrictBiome = !ETFConfigScreenMain.temporaryETFConfig.restrictBiome;
                    button.setMessage(Text.of(ETFVersionDifferenceHandler.getTextFromTranslation(
                            "config." + ETFClientCommon.MOD_ID + ".restrict_biome.title"
                    ).getString() + ": " + (ETFConfigScreenMain.temporaryETFConfig.restrictBiome ? ScreenTexts.ON : ScreenTexts.OFF).getString()));
                },
                ETFVersionDifferenceHandler.getTextFromTranslation("config." + ETFClientCommon.MOD_ID + ".restrict_biome.tooltip")
        );
        ButtonWidget restrictHeight = getETFButton((int) (this.width * 0.525), (int) (this.height * 0.4), (int) (this.width * 0.2), 20,
                Text.of(ETFVersionDifferenceHandler.getTextFromTranslation(
                        "config." + ETFClientCommon.MOD_ID + ".restrict_height.title"
                ).getString() + ": " + (ETFConfigScreenMain.temporaryETFConfig.restrictHeight ? ScreenTexts.ON : ScreenTexts.OFF).getString()),
                (button) -> {
                    ETFConfigScreenMain.temporaryETFConfig.restrictHeight = !ETFConfigScreenMain.temporaryETFConfig.restrictHeight;
                    button.setMessage(Text.of(ETFVersionDifferenceHandler.getTextFromTranslation(
                            "config." + ETFClientCommon.MOD_ID + ".restrict_height.title"
                    ).getString() + ": " + (ETFConfigScreenMain.temporaryETFConfig.restrictHeight ? ScreenTexts.ON : ScreenTexts.OFF).getString()));
                },
                ETFVersionDifferenceHandler.getTextFromTranslation("config." + ETFClientCommon.MOD_ID + ".restrict_height.tooltip")
        );
        ButtonWidget restrictBlock = getETFButton((int) (this.width * 0.275), (int) (this.height * 0.5), (int) (this.width * 0.2), 20,
                Text.of(ETFVersionDifferenceHandler.getTextFromTranslation(
                        "config." + ETFClientCommon.MOD_ID + ".restrict_block.title"
                ).getString() + ": " + (ETFConfigScreenMain.temporaryETFConfig.restrictBlock ? ScreenTexts.ON : ScreenTexts.OFF).getString()),
                (button) -> {
                    ETFConfigScreenMain.temporaryETFConfig.restrictBlock = !ETFConfigScreenMain.temporaryETFConfig.restrictBlock;
                    button.setMessage(Text.of(ETFVersionDifferenceHandler.getTextFromTranslation(
                            "config." + ETFClientCommon.MOD_ID + ".restrict_block.title"
                    ).getString() + ": " + (ETFConfigScreenMain.temporaryETFConfig.restrictBlock ? ScreenTexts.ON : ScreenTexts.OFF).getString()));
                },
                ETFVersionDifferenceHandler.getTextFromTranslation("config." + ETFClientCommon.MOD_ID + ".restrict_block.tooltip")
        );
        ButtonWidget restrictWeather = getETFButton((int) (this.width * 0.525), (int) (this.height * 0.5), (int) (this.width * 0.2), 20,
                Text.of(ETFVersionDifferenceHandler.getTextFromTranslation(
                        "config." + ETFClientCommon.MOD_ID + ".restrict_weather.title"
                ).getString() + ": " + (ETFConfigScreenMain.temporaryETFConfig.restrictWeather ? ScreenTexts.ON : ScreenTexts.OFF).getString()),
                (button) -> {
                    ETFConfigScreenMain.temporaryETFConfig.restrictWeather = !ETFConfigScreenMain.temporaryETFConfig.restrictWeather;
                    button.setMessage(Text.of(ETFVersionDifferenceHandler.getTextFromTranslation(
                            "config." + ETFClientCommon.MOD_ID + ".restrict_weather.title"
                    ).getString() + ": " + (ETFConfigScreenMain.temporaryETFConfig.restrictWeather ? ScreenTexts.ON : ScreenTexts.OFF).getString()));
                },
                ETFVersionDifferenceHandler.getTextFromTranslation("config." + ETFClientCommon.MOD_ID + ".restrict_weather.tooltip")
        );
        ButtonWidget restrictTime = getETFButton((int) (this.width * 0.275), (int) (this.height * 0.6), (int) (this.width * 0.2), 20,
                Text.of(ETFVersionDifferenceHandler.getTextFromTranslation(
                        "config." + ETFClientCommon.MOD_ID + ".restrict_day_time.title"
                ).getString() + ": " + (ETFConfigScreenMain.temporaryETFConfig.restrictDayTime ? ScreenTexts.ON : ScreenTexts.OFF).getString()),
                (button) -> {
                    ETFConfigScreenMain.temporaryETFConfig.restrictDayTime = !ETFConfigScreenMain.temporaryETFConfig.restrictDayTime;
                    button.setMessage(Text.of(ETFVersionDifferenceHandler.getTextFromTranslation(
                            "config." + ETFClientCommon.MOD_ID + ".restrict_day_time.title"
                    ).getString() + ": " + (ETFConfigScreenMain.temporaryETFConfig.restrictDayTime ? ScreenTexts.ON : ScreenTexts.OFF).getString()));
                },
                ETFVersionDifferenceHandler.getTextFromTranslation("config." + ETFClientCommon.MOD_ID + ".restrict_day_time.tooltip")
        );
        ButtonWidget restrictMoon = getETFButton((int) (this.width * 0.525), (int) (this.height * 0.6), (int) (this.width * 0.2), 20,
                Text.of(ETFVersionDifferenceHandler.getTextFromTranslation(
                        "config." + ETFClientCommon.MOD_ID + ".restrict_moon_phase.title"
                ).getString() + ": " + (ETFConfigScreenMain.temporaryETFConfig.restrictMoonPhase ? ScreenTexts.ON : ScreenTexts.OFF).getString()),
                (button) -> {
                    ETFConfigScreenMain.temporaryETFConfig.restrictMoonPhase = !ETFConfigScreenMain.temporaryETFConfig.restrictMoonPhase;
                    button.setMessage(Text.of(ETFVersionDifferenceHandler.getTextFromTranslation(
                            "config." + ETFClientCommon.MOD_ID + ".restrict_moon_phase.title"
                    ).getString() + ": " + (ETFConfigScreenMain.temporaryETFConfig.restrictMoonPhase ? ScreenTexts.ON : ScreenTexts.OFF).getString()));
                },
                ETFVersionDifferenceHandler.getTextFromTranslation("config." + ETFClientCommon.MOD_ID + ".restrict_moon_phase.tooltip")
        );

        this.addDrawableChild(getETFButton((int) (this.width * 0.27), (int) (this.height * 0.2), (int) (this.width * 0.45), 20,
                Text.of(ETFVersionDifferenceHandler.getTextFromTranslation(
                        "config." + ETFClientCommon.MOD_ID + ".restrict_update_properties.title"
                ).getString() + ": " + (ETFConfigScreenMain.temporaryETFConfig.restrictUpdateProperties ? ScreenTexts.ON : ScreenTexts.OFF).getString()),
                (button) -> {
                    ETFConfigScreenMain.temporaryETFConfig.restrictUpdateProperties = !ETFConfigScreenMain.temporaryETFConfig.restrictUpdateProperties;
                    button.setMessage(Text.of(ETFVersionDifferenceHandler.getTextFromTranslation(
                            "config." + ETFClientCommon.MOD_ID + ".restrict_update_properties.title"
                    ).getString() + ": " + (ETFConfigScreenMain.temporaryETFConfig.restrictUpdateProperties ? ScreenTexts.ON : ScreenTexts.OFF).getString()));

                    restrictBiome.active = ETFConfigScreenMain.temporaryETFConfig.restrictUpdateProperties;
                    restrictBlock.active = ETFConfigScreenMain.temporaryETFConfig.restrictUpdateProperties;
                    restrictHeight.active = ETFConfigScreenMain.temporaryETFConfig.restrictUpdateProperties;
                    restrictWeather.active = ETFConfigScreenMain.temporaryETFConfig.restrictUpdateProperties;
                    restrictTime.active = ETFConfigScreenMain.temporaryETFConfig.restrictUpdateProperties;
                    restrictMoon.active = ETFConfigScreenMain.temporaryETFConfig.restrictUpdateProperties;
                },
                ETFVersionDifferenceHandler.getTextFromTranslation("config." + ETFClientCommon.MOD_ID + ".restrict_update_properties.tooltip")
        ));

        restrictBiome.active = ETFConfigScreenMain.temporaryETFConfig.restrictUpdateProperties;
        restrictBlock.active = ETFConfigScreenMain.temporaryETFConfig.restrictUpdateProperties;
        restrictHeight.active = ETFConfigScreenMain.temporaryETFConfig.restrictUpdateProperties;
        restrictWeather.active = ETFConfigScreenMain.temporaryETFConfig.restrictUpdateProperties;
        restrictTime.active = ETFConfigScreenMain.temporaryETFConfig.restrictUpdateProperties;
        restrictMoon.active = ETFConfigScreenMain.temporaryETFConfig.restrictUpdateProperties;

        this.addDrawableChild(restrictBiome);
        this.addDrawableChild(restrictBlock);
        this.addDrawableChild(restrictHeight);
        this.addDrawableChild(restrictWeather);
        this.addDrawableChild(restrictTime);
        this.addDrawableChild(restrictMoon);


    }


    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);

        drawCenteredText(matrices, textRenderer, ETFVersionDifferenceHandler.getTextFromTranslation("config." + ETFClientCommon.MOD_ID + ".restriction_settings.title"), (int) (width * 0.5), (int) (height * 0.35), 0xFFFFFF);

    }

}

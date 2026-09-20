import {
    Card,
    CardContent,
    FormControl,
    InputLabel,
    MenuItem,
    Select,
    Stack,
    Typography,
} from "@mui/material";

import LanguageIcon from "@mui/icons-material/Language";

import ReactCountryFlag from "react-country-flag";

import { useTranslation } from "react-i18next";

interface LanguageOption {
    code: string;
    label: string;
    countryCode: string;
}

const languages: LanguageOption[] = [
    {
        code: "en",
        label: "English",
        countryCode: "GB",
    },
    {
        code: "no",
        label: "Norsk",
        countryCode: "NO",
    },
];

export default function LanguageSelectionCard() {
    const { t, i18n } = useTranslation();

    const normalizedLanguage =
        (
            i18n.language ||
            "en"
        )
            .split("-")[0]
            .toLowerCase();

    const currentLanguage =
        normalizedLanguage === "nb"
            ? "no"
            : languages.some(
                language =>
                    language.code ===
                    normalizedLanguage
            )
                ? normalizedLanguage
                : "en";

    const selectedLanguage =
        languages.find(
            language =>
                language.code ===
                currentLanguage
        ) ?? languages[0];

    const handleLanguageChange = (
        language: string
    ): void => {
        void i18n.changeLanguage(language);
    };

    return (
        <Card
            sx={{
                borderRadius: 3,
            }}
        >
            <CardContent
                sx={{
                    p: {
                        xs: 2.5,
                        sm: 3,
                    },
                    "&:last-child": {
                        pb: {
                            xs: 2.5,
                            sm: 3,
                        },
                    },
                }}
            >
                <Stack
                    direction={{
                        xs: "column",
                        sm: "row",
                    }}
                    spacing={2}
                    sx={{
                        alignItems: {
                            xs: "stretch",
                            sm: "center",
                        },
                    }}
                >
                    <Stack
                        direction="row"
                        spacing={1.5}
                        sx={{
                            alignItems: "center",
                            flex: 1,
                            minWidth: 0,
                        }}
                    >
                        <LanguageIcon
                            color="action"
                        />

                        <Stack spacing={0.25}>
                            <Typography
                                sx={{
                                    fontWeight: 600,
                                }}
                            >
                                {t(
                                    "settings.language.title"
                                )}
                            </Typography>

                            <Typography
                                variant="body2"
                                color="text.secondary"
                            >
                                {t(
                                    "settings.language.body"
                                )}
                            </Typography>
                        </Stack>
                    </Stack>

                    <FormControl
                        size="small"
                        sx={{
                            minWidth: {
                                xs: "100%",
                                sm: 180,
                            },
                        }}
                    >
                        <InputLabel>
                            {t(
                                "settings.language.title"
                            )}
                        </InputLabel>

                        <Select
                            value={currentLanguage}
                            label={t(
                                "settings.language.title"
                            )}
                            onChange={event => {
                                handleLanguageChange(
                                    event.target.value
                                );
                            }}
                            renderValue={() => (
                                <Stack
                                    direction="row"
                                    spacing={1}
                                    sx={{
                                        alignItems:
                                            "center",
                                    }}
                                >
                                    <ReactCountryFlag
                                        countryCode={
                                            selectedLanguage.countryCode
                                        }
                                        svg
                                        style={{
                                            width:
                                                "1.25em",
                                            height:
                                                "1.25em",
                                        }}
                                    />

                                    <Typography
                                        component="span"
                                    >
                                        {
                                            selectedLanguage.label
                                        }
                                    </Typography>
                                </Stack>
                            )}
                        >
                            {languages.map(
                                language => (
                                    <MenuItem
                                        key={
                                            language.code
                                        }
                                        value={
                                            language.code
                                        }
                                    >
                                        <Stack
                                            direction="row"
                                            spacing={1}
                                            sx={{
                                                alignItems:
                                                    "center",
                                            }}
                                        >
                                            <ReactCountryFlag
                                                countryCode={
                                                    language.countryCode
                                                }
                                                svg
                                                style={{
                                                    width:
                                                        "1.25em",
                                                    height:
                                                        "1.25em",
                                                }}
                                            />

                                            <Typography
                                                component="span"
                                            >
                                                {
                                                    language.label
                                                }
                                            </Typography>
                                        </Stack>
                                    </MenuItem>
                                )
                            )}
                        </Select>
                    </FormControl>
                </Stack>
            </CardContent>
        </Card>
    );
}
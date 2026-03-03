#include "MedicineData.h"

const String MedicineData::names[NUMBER_OF_CONTAINERS] = {
    "Paracetamol", "Amoxicillin", "Ibuprofen",
    "Cetirizine", "Metformin", "Vitamin C"
};

const String MedicineData::expirations[NUMBER_OF_CONTAINERS] = {
    "EXP: 06/2026", "EXP: 11/2025", "EXP: 02/2027",
    "EXP: 09/2026", "EXP: 01/2028", "EXP: 12/2025"
};

MedicineData::MedicineData() {}

String MedicineData::getName(int index) const {
    return (index >= 0 && index < NUMBER_OF_CONTAINERS) ? names[index] : "";
}

String MedicineData::getExpiration(int index) const {
    return (index >= 0 && index < NUMBER_OF_CONTAINERS) ? expirations[index] : "";
}

int MedicineData::getCount() const {
    return NUMBER_OF_CONTAINERS;
}
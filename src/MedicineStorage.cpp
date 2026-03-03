#include "MedicineStorage.h"
#include <Arduino.h>

MedicineStorage::MedicineStorage() {}

void MedicineStorage::clear() {
    medicines.clear();
}

void MedicineStorage::addOrUpdate(const MedicineEntry& med) {
    for (auto& m : medicines) {
        if (m.id == med.id) {
            m = med;
            return;
        }
    }
    medicines.push_back(med);
}

const MedicineEntry* MedicineStorage::getByPillbox(int pillboxNumber) const {
    for (const auto& m : medicines) {
        if (m.pillboxNumber == pillboxNumber) {
            return &m;
        }
    }
    return nullptr;
}

const MedicineEntry* MedicineStorage::getById(int id) const {
    for (const auto& m : medicines) {
        if (m.id == id) {
            return &m;
        }
    }
    return nullptr;
}

void MedicineStorage::printAll() const {
    Serial.println("Stored medicines:");
    for (const auto& m : medicines) {
        Serial.printf("Pillbox %d: %s @ %s\n", m.pillboxNumber, m.brandName.c_str(), m.intakeTime.c_str());
    }
}

bool MedicineStorage::hasMedicineDueAt(int hour, int minute) const {
    char timeStr[6];
    sprintf(timeStr, "%02d:%02d", hour, minute);
    String targetTime = String(timeStr);
    
    for (const auto& med : medicines) {
        if (med.remindersEnabled && med.intakeTime == targetTime) {
            return true;
        }
    }
    return false;
}

int MedicineStorage::getDuePillbox(int hour, int minute) const {
    char timeStr[6];
    sprintf(timeStr, "%02d:%02d", hour, minute);
    String targetTime = String(timeStr);
    
    for (const auto& med : medicines) {
        if (med.remindersEnabled && med.intakeTime == targetTime) {
            return med.pillboxNumber; // 1..6
        }
    }
    return 0;
}
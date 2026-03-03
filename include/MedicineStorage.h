#ifndef MEDICINE_STORAGE_H
#define MEDICINE_STORAGE_H

#include <Arduino.h>
#include <vector>

struct MedicineEntry {
    int id;
    String brandName;
    String genericName;
    String dosage;
    int quantity;
    uint64_t expiryDate;        // milliseconds since epoch
    String intakeTime;
    bool remindersEnabled;
    String notes;
    int pillboxNumber;
    uint64_t timestamp;         // milliseconds since epoch
};

class MedicineStorage {
public:
    MedicineStorage();
    void clear();
    void addOrUpdate(const MedicineEntry& med);
    const MedicineEntry* getByPillbox(int pillboxNumber) const;   // 1..6
    const MedicineEntry* getById(int id) const;
    int getCount() const { return medicines.size(); }
    void printAll() const;
    bool hasMedicineDueAt(int hour, int minute) const;
    int getDuePillbox(int hour, int minute) const;   // returns pillbox number (1..6) or 0 if none

private:
    std::vector<MedicineEntry> medicines;
};

#endif
#ifndef MEDICINE_DATA_H
#define MEDICINE_DATA_H

#include <Arduino.h>
#include "Config.h"

class MedicineData {
public:
    MedicineData();
    String getName(int index) const;
    String getExpiration(int index) const;
    int getCount() const;

private:
    static const String names[NUMBER_OF_CONTAINERS];
    static const String expirations[NUMBER_OF_CONTAINERS];
};

#endif
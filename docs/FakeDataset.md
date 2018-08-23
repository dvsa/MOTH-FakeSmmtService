# Fake data returned by service
Depend from requested marque fake SMMT service will return that there is a outstanding recall or not.

### Marques with outstanding recall

* RENAULT
* VOLKSWAGEN
* ISUZU
* SUBARU

### Marques without outstanding recall

* BMW
* AUDI
* PEUGEOT (with 16 seconds delay)
* VOLVO (with 3 seconds delay)
* DAIHATSU (with 3 seconds delay)
* GREAT WALL

### Other marques
Other marques are not stored in fake SMMT service and they will return "Bad Request - Invalid Marque" message.

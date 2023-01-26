package edu.gtri.gpssample.models

import edu.gtri.gpssample.constants.FieldType

class Field
{
    var id: Int = -1
    var studyId: Int = -1
    var name: String = ""
    var type: FieldType = FieldType.Text
    var pii: Int = 0
    var required: Int = 0
    var integerOnly: Int = 0
    var date: Int = 0
    var time: Int = 0
    var option1: String = ""
    var option2: String = ""
    var option3: String = ""
    var option4: String = ""
}
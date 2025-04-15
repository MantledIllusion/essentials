# string-essentials

Offers recursive placeholder replacement for Strings.

## Syntax 

The default syntax uses `${` as placeholder prefix, `}` as suffix and `:` as an optional infix for default placeholders.

The example text ...

````text
Undesireable No 1, Mr. Harry ${names.${mode}: }Potter.
````

... renders very differently depending on the following variables, allowing for simple conditional rendering logic:

| Available Placeholders                                                               | Rendered Text                                  |
|--------------------------------------------------------------------------------------|------------------------------------------------|
|                                                                                      | Undesireable No 1, Mr. Harry Potter            |
| mode = "none"                                                                        | Undesireable No 1, Mr. Harry Potter            |
| mode = "none"<br>names.middleName = "James "<br>names.nickName = "Patronus "         | Undesireable No 1, Mr. Harry Potter            |
| mode = "middleName"<br>names.middleName = "James "<br>names.nickName = "'Patronus' " | Undesireable No 1, Mr. Harry James Potter      |
| mode = "nickName"<br>names.middleName = "James "<br>names.nickName = "'Patronus' "   | Undesireable No 1, Mr. Harry 'Patronus' Potter |

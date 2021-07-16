SELECT json_build_array(
   json_build_object('id', '457daad5-4f1b-4eb7-80ec-6882adb8cc7d', 'name', ?::text),
   json_build_object('id', '6085c12e-e94d-4ae1-b7ad-23acc7a82a98', 'name', ?::text || '-2')
)
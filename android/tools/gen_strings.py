#!/usr/bin/env python3
"""Genera res/values*/strings.xml desde una única tabla (es, eu, gl, ca).
Las traducciones eu/gl/ca son un primer borrador y requieren revisión nativa."""
import os
from xml.sax.saxutils import escape

S = {}
def s(key, es, eu, gl, ca):
    S[key] = dict(es=es, eu=eu, gl=gl, ca=ca)

s('app_name', 'Mus', 'Mus', 'Mus', 'Mus')
s('app_title', 'Mus', 'Mus', 'Mus', 'Mus')
s('tagline', 'Mus por parejas, a 8 reyes', 'Bikoteka mus, 8 erregerekin', 'Mus por parellas, a 8 reis', 'Mus per parelles, a 8 reis')
s('back', 'Atrás', 'Atzera', 'Atrás', 'Enrere')
s('cancel', 'Cancelar', 'Utzi', 'Cancelar', 'Cancel·la')
s('next', 'Siguiente', 'Hurrengoa', 'Seguinte', 'Següent')
s('skip', 'Saltar', 'Saltatu', 'Saltar', 'Omet')
s('start', 'Empezar', 'Hasi', 'Comezar', 'Comença')
s('onb1_title', 'Mus por parejas', 'Bikoteka mus', 'Mus por parellas', 'Mus per parelles')
s('onb1_body', 'Juegas con tu compañero de enfrente contra la pareja rival. Gana la primera pareja que llegue a 40 tantos.',
  'Aurrean duzun lagunarekin jokatzen duzu aurkako bikotearen kontra. 40 tantora iristen den lehen bikoteak irabazten du.',
  'Xogas co teu compañeiro de enfronte contra a parella rival. Gaña a primeira parella que chegue a 40 tantos.',
  'Jugues amb el company del davant contra la parella rival. Guanya la primera parella que arribi a 40 punts.')
s('onb2_title', 'Cuatro lances', 'Lau jokaldi', 'Catro lances', 'Quatre lances')
s('onb2_body', 'En cada mano se juegan grande, chica, pares y juego (o punto). Envida, quiere o lánzate al órdago.',
  'Esku bakoitzean handia, txikia, pareak eta jokoa (edo puntua) jokatzen dira. Enbidatu, onartu edo bota hordagoa.',
  'En cada man xóganse grande, chica, pares e xogo (ou punto). Envida, quere ou lánzate ao órdago.',
  'A cada mà es juguen grande, chica, parells i joc (o punt). Envida, vol o llança\'t a l\'òrdago.')
s('onb3_title', 'Señas con tu compañero', 'Keinuak lagunarekin', 'Sinais co teu compañeiro', 'Senyes amb el company')
s('onb3_body', 'Haz señas para contar a tu pareja qué llevas: sólo las ve tu compañero. Aquí no hay apuestas: los tantos son sólo puntuación.',
  'Egin keinuak lagunari zer daukazun esateko: lagunak bakarrik ikusten ditu. Hemen ez dago apusturik: tantoak puntuazioa baino ez dira.',
  'Fai sinais para contarlle á túa parella o que levas: só as ve o teu compañeiro. Aquí non hai apostas: os tantos son só puntuación.',
  'Fes senyes per dir al company què portes: només les veu ell. Aquí no hi ha apostes: els punts són només puntuació.')
s('login_google_soon', 'Entrar con Google (próximamente)', 'Sartu Googlerekin (laster)', 'Entrar con Google (proximamente)', 'Entra amb Google (aviat)')
s('login_email_soon', 'Entrar con email (próximamente)', 'Sartu emailarekin (laster)', 'Entrar con correo (proximamente)', 'Entra amb correu (aviat)')
s('login_guest_title', 'Jugar como invitado', 'Gonbidatu gisa jokatu', 'Xogar como convidado', 'Juga com a convidat')
s('public_name', 'Nombre público', 'Izen publikoa', 'Nome público', 'Nom públic')
s('public_name_hint', 'Entre 2 y 20 caracteres. Lo verán los demás jugadores.', '2 eta 20 karaktere artean. Beste jokalariek ikusiko dute.', 'Entre 2 e 20 caracteres. Verano os demais xogadores.', 'Entre 2 i 20 caràcters. El veuran els altres jugadors.')
s('consent_analytics', 'Acepto enviar estadísticas de uso anónimas para mejorar el juego', 'Onartzen dut erabilera-estatistika anonimoak bidaltzea jokoa hobetzeko', 'Acepto enviar estatísticas de uso anónimas para mellorar o xogo', 'Accepto enviar estadístiques d\'ús anònimes per millorar el joc')
s('login_guest', 'Continuar', 'Jarraitu', 'Continuar', 'Continua')
s('no_gambling_notice', 'Juego sin dinero real: no hay apuestas, premios ni fichas canjeables.', 'Benetako dirurik gabeko jokoa: ez dago apusturik, saririk ez fitxa trukagarririk.', 'Xogo sen diñeiro real: non hai apostas, premios nin fichas canxeables.', 'Joc sense diners reals: no hi ha apostes, premis ni fitxes bescanviables.')
s('hello_name', 'Hola, %1$s', 'Kaixo, %1$s', 'Ola, %1$s', 'Hola, %1$s')
s('profile', 'Perfil', 'Profila', 'Perfil', 'Perfil')
s('settings', 'Ajustes', 'Ezarpenak', 'Axustes', 'Configuració')
s('play_bots', 'Partida contra bots', 'Partida boten aurka', 'Partida contra bots', 'Partida contra bots')
s('play_bots_desc', 'Tú y un bot compañero contra dos bots. Funciona sin conexión.', 'Zu eta bot lagun bat bi boten aurka. Konexiorik gabe dabil.', 'Ti e un bot compañeiro contra dous bots. Funciona sen conexión.', 'Tu i un bot company contra dos bots. Funciona sense connexió.')
s('play_now', 'Jugar', 'Jokatu', 'Xogar', 'Juga')
s('level_facil', 'Fácil', 'Erraza', 'Fácil', 'Fàcil')
s('level_normal', 'Normal', 'Normala', 'Normal', 'Normal')
s('level_dificil', 'Difícil', 'Zaila', 'Difícil', 'Difícil')
s('quick_match', 'Partida rápida', 'Partida azkarra', 'Partida rápida', 'Partida ràpida')
s('quick_match_desc', 'Emparejamiento por nivel con jugadores reales', 'Mailaren araberako parekatzea benetako jokalariekin', 'Emparellamento por nivel con xogadores reais', 'Aparellament per nivell amb jugadors reals')
s('private_room', 'Sala privada', 'Gela pribatua', 'Sala privada', 'Sala privada')
s('private_room_desc', 'Crea una mesa y comparte el enlace por WhatsApp', 'Sortu mahai bat eta partekatu esteka WhatsApp bidez', 'Crea unha mesa e comparte a ligazón por WhatsApp', 'Crea una taula i comparteix l\'enllaç per WhatsApp')
s('leagues', 'Ligas de cuadrilla', 'Koadrilako ligak', 'Ligas de pandilla', 'Lligues de colla')
s('leagues_desc', 'Temporadas y clasificación con tus amigos', 'Denboraldiak eta sailkapena lagunekin', 'Tempadas e clasificación cos teus amigos', 'Temporades i classificació amb els amics')
s('shop', 'Tienda y Socio', 'Denda eta Bazkidea', 'Tenda e Socio', 'Botiga i Soci')
s('shop_desc', 'Barajas y tapetes. Sin cajas de botín.', 'Karta-sortak eta tapeteak. Harrapakin-kutxarik gabe.', 'Barallas e tapetes. Sen caixas de botín.', 'Baralles i tapets. Sense caixes de botí.')
s('soon', 'PRÓXIMAMENTE', 'LASTER', 'PROXIMAMENTE', 'AVIAT')
s('proto_notice', 'Prototipo: las partidas online, el ranking y las compras llegarán en las próximas fases.', 'Prototipoa: online partidak, sailkapena eta erosketak hurrengo faseetan iritsiko dira.', 'Prototipo: as partidas en liña, o ranking e as compras chegarán nas próximas fases.', 'Prototip: les partides en línia, el rànquing i les compres arribaran en les properes fases.')
s('soon_quick', 'La partida rápida con emparejamiento por rating (Glicko-2) llega en la fase 4. Si falta gente, los huecos se rellenan con bots.', 'Rating bidezko (Glicko-2) partida azkarra 4. fasean iritsiko da. Jende falta bada, botek beteko dituzte hutsuneak.', 'A partida rápida con emparellamento por rating (Glicko-2) chega na fase 4. Se falta xente, os ocos énchense con bots.', 'La partida ràpida amb aparellament per rating (Glicko-2) arriba a la fase 4. Si falta gent, els forats s\'omplen amb bots.')
s('soon_private', 'Las salas privadas por código o enlace llegan con el servidor (fases 2 y 3).', 'Kode edo esteka bidezko gela pribatuak zerbitzariarekin iritsiko dira (2. eta 3. faseak).', 'As salas privadas por código ou ligazón chegan co servidor (fases 2 e 3).', 'Les sales privades per codi o enllaç arriben amb el servidor (fases 2 i 3).')
s('soon_leagues', 'Las ligas privadas con temporadas, clasificación e historial llegan en la fase 4.', 'Denboraldi, sailkapen eta historialdun liga pribatuak 4. fasean iritsiko dira.', 'As ligas privadas con tempadas, clasificación e historial chegan na fase 4.', 'Les lligues privades amb temporades, classificació i historial arriben a la fase 4.')
s('soon_shop', 'La suscripción Socio y los cosméticos (barajas y tapetes) llegan en la fase 5. Nada aleatorio de pago y nunca dinero real.', 'Bazkide harpidetza eta kosmetikoak (karta-sortak eta tapeteak) 5. fasean iritsiko dira. Ezer ez ausazko ordainpekorik, eta inoiz ez benetako dirurik.', 'A subscrición Socio e os cosméticos (barallas e tapetes) chegan na fase 5. Nada aleatorio de pago e nunca diñeiro real.', 'La subscripció Soci i els cosmètics (baralles i tapets) arriben a la fase 5. Res aleatori de pagament i mai diners reals.')
s('bot_right', 'Koldo', 'Koldo', 'Koldo', 'Koldo')
s('bot_partner', 'Maite', 'Maite', 'Maite', 'Maite')
s('bot_left', 'Rosi', 'Rosi', 'Rosi', 'Rosi')
s('team_us', 'Nosotros', 'Gu', 'Nós', 'Nosaltres')
s('team_them', 'Ellos', 'Haiek', 'Eles', 'Ells')
s('history', 'Historial', 'Historia', 'Historial', 'Historial')
s('score_desc', '%1$s: %2$d de %3$d tantos', '%1$s: %2$d/%3$d tanto', '%1$s: %2$d de %3$d tantos', '%1$s: %2$d de %3$d punts')
s('lance_grande', 'Grande', 'Handia', 'Grande', 'Grande')
s('lance_chica', 'Chica', 'Txikia', 'Chica', 'Chica')
s('lance_pares', 'Pares', 'Pareak', 'Pares', 'Parells')
s('lance_juego', 'Juego', 'Jokoa', 'Xogo', 'Joc')
s('lance_punto', 'Punto', 'Puntua', 'Punto', 'Punt')
s('phase_mus', 'Mus', 'Mus', 'Mus', 'Mus')
s('phase_descarte', 'Descarte', 'Deskartea', 'Descarte', 'Descart')
s('phase_fin_mano', 'Fin de mano', 'Eskuaren amaiera', 'Fin da man', 'Final de mà')
s('phase_fin_partida', 'Fin de partida', 'Partidaren amaiera', 'Fin da partida', 'Final de partida')
s('short_ordago', 'Órdago', 'Hordago', 'Órdago', 'Òrdago')
s('short_paso', 'En paso', 'Pasoan', 'En paso', 'En pas')
s('short_querido', 'Querido %1$d', 'Onartua %1$d', 'Querido %1$d', 'Volgut %1$d')
s('short_no_querido', 'No quiso (%1$d)', 'Ez onartua (%1$d)', 'Non quixo (%1$d)', 'No volgut (%1$d)')
s('short_sin_apuesta', 'Sin envite', 'Enbidorik ez', 'Sen envite', 'Sense envit')
s('short_no_hay', 'No hay', 'Ez dago', 'Non hai', 'No n\'hi ha')
s('badge_mano', 'MANO', 'ESKU', 'MAN', 'MÀ')
s('badge_dealer', 'Reparte', 'Banatzailea', 'Reparte', 'Reparteix')
s('badge_pares_si', 'Pares sí', 'Pareak bai', 'Pares si', 'Parells sí')
s('badge_pares_no', 'Pares no', 'Pareak ez', 'Pares non', 'Parells no')
s('badge_juego_si', 'Juego sí', 'Jokoa bai', 'Xogo si', 'Joc sí')
s('badge_juego_no', 'Juego no', 'Jokoa ez', 'Xogo non', 'Joc no')
s('discarded_n', 'Cambió %1$d', '%1$d aldatu', 'Cambiou %1$d', 'N\'ha canviat %1$d')
s('discarding', 'Descartando…', 'Deskartatzen…', 'Descartando…', 'Descartant…')
s('discard_done', 'Listo', 'Prest', 'Listo', 'A punt')
s('ordago_on_table', '¡ÓRDAGO!', 'HORDAGO!', 'ÓRDAGO!', 'ÒRDAGO!')
s('bet_on_table', 'Envite: %1$d', 'Enbidoa: %1$d', 'Envite: %1$d', 'Envit: %1$d')
s('deje_info', 'Si no se quiere: %1$d', 'Onartzen ez bada: %1$d', 'Se non se quere: %1$d', 'Si no es vol: %1$d')
s('mus_round', 'Ronda de mus %1$d', '%1$d. mus txanda', 'Rolda de mus %1$d', 'Ronda de mus %1$d')
s('hand_n', 'Mano %1$d', '%1$d. eskua', 'Man %1$d', 'Mà %1$d')
s('my_points', '%1$d puntos', '%1$d puntu', '%1$d puntos', '%1$d punts')
s('senas', 'Señas', 'Keinuak', 'Sinais', 'Senyes')
s('waiting_discards', 'Esperando descartes…', 'Deskarteen zain…', 'Agardando descartes…', 'Esperant descarts…')
s('waiting_for', 'Turno de %1$s…', '%1$s(r)en txanda…', 'Quenda de %1$s…', 'Torn de %1$s…')
s('discard_hint', 'Toca las cartas que quieres cambiar', 'Ukitu aldatu nahi dituzun kartak', 'Toca as cartas que queres cambiar', 'Toca les cartes que vols canviar')
s('discard_n', 'Descartar (%1$d)', 'Deskartatu (%1$d)', 'Descartar (%1$d)', 'Descarta (%1$d)')
s('act_mus', 'Mus', 'Mus', 'Mus', 'Mus')
s('act_no_mus', 'No hay mus', 'Musik ez', 'Non hai mus', 'No hi ha mus')
s('act_descarto', 'Cambio %1$d', '%1$d aldatu', 'Cambio %1$d', 'En canvio %1$d')
s('act_paso', 'Paso', 'Paso', 'Paso', 'Passo')
s('act_envido_n', 'Envido %1$d', 'Enbido %1$d', 'Envido %1$d', 'Envido %1$d')
s('act_raise_n', '%1$d más', '%1$d gehiago', '%1$d máis', '%1$d més')
s('act_quiero', 'Quiero', 'Iduki', 'Quero', 'Vull')
s('act_no_quiero', 'No quiero', 'Ez dut nahi', 'Non quero', 'No vull')
s('act_ordago', '¡Órdago!', 'Hordago!', 'Órdago!', 'Òrdago!')
s('act_quiero_ordago', '¡Quiero el órdago!', 'Hordagoa iduki!', 'Quero o órdago!', 'Vull l\'òrdago!')
s('act_sena', '%1$s', '%1$s', '%1$s', '%1$s')
s('next_hand', 'Siguiente mano', 'Hurrengo eskua', 'Seguinte man', 'Mà següent')
s('rematch', 'Revancha', 'Mendekua', 'Revancha', 'Revenja')
s('you_win', '¡Habéis ganado!', 'Irabazi duzue!', 'Gañastedes!', 'Heu guanyat!')
s('you_lose', 'Han ganado ellos', 'Haiek irabazi dute', 'Gañaron eles', 'Han guanyat ells')
s('hand_end_n', 'Fin de la mano %1$d', '%1$d. eskuaren amaiera', 'Fin da man %1$d', 'Final de la mà %1$d')
s('score_line', 'Nosotros %1$d – %2$d Ellos', 'Gu %1$d – %2$d Haiek', 'Nós %1$d – %2$d Eles', 'Nosaltres %1$d – %2$d Ells')
s('out_ordago_querido', 'Órdago querido', 'Hordagoa onartua', 'Órdago querido', 'Òrdago volgut')
s('out_paso', 'En paso', 'Pasoan', 'En paso', 'En pas')
s('out_querido', 'Querido a %1$d', '%1$d(e)ra onartua', 'Querido a %1$d', 'Volgut a %1$d')
s('out_no_querido', 'No querido (+%1$d de deje)', 'Ez onartua (+%1$d)', 'Non querido (+%1$d de deixe)', 'No volgut (+%1$d)')
s('out_sin_apuesta', 'Sólo una pareja lo tiene: sin envite', 'Bikote batek bakarrik: enbidorik ez', 'Só unha parella o ten: sen envite', 'Només una parella en té: sense envit')
s('out_no_hay', 'Nadie tiene', 'Inork ez du', 'Ninguén ten', 'Ningú no en té')
s('award_deje', 'deje', 'uztea', 'deixe', 'deixada')
s('award_apuesta', 'envite', 'enbidoa', 'envite', 'envit')
s('award_paso', 'en paso', 'pasoan', 'en paso', 'en pas')
s('award_jugada', 'jugada', 'jokaldia', 'xogada', 'jugada')
s('award_ordago', 'órdago', 'hordagoa', 'órdago', 'òrdago')
s('exit_title', '¿Abandonar la partida?', 'Partida utzi?', 'Abandonar a partida?', 'Abandonar la partida?')
s('exit_body', 'Perderás esta partida. En las partidas online, abandonar penaliza.', 'Partida hau galduko duzu. Online partidetan, uzteak zigorra dakar.', 'Perderás esta partida. Nas partidas en liña, abandonar penaliza.', 'Perdràs aquesta partida. A les partides en línia, abandonar penalitza.')
s('exit_confirm', 'Salir', 'Irten', 'Saír', 'Surt')
s('history_hand', 'Mano %1$d', '%1$d. eskua', 'Man %1$d', 'Mà %1$d')
s('history_past', 'Manos anteriores', 'Aurreko eskuak', 'Mans anteriores', 'Mans anteriors')
s('history_hand_line', 'Mano %1$d: Nosotros +%2$d · Ellos +%3$d', '%1$d. eskua: Gu +%2$d · Haiek +%3$d', 'Man %1$d: Nós +%2$d · Eles +%3$d', 'Mà %1$d: Nosaltres +%2$d · Ells +%3$d')
s('senas_title', 'Hacer una seña', 'Keinu bat egin', 'Facer un sinal', 'Fer una senya')
s('senas_hint', 'Sólo la ve tu compañero; los rivales no se enteran.', 'Lagunak bakarrik ikusten du; aurkariak ez dira konturatzen.', 'Só a ve o teu compañeiro; os rivais non se decatan.', 'Només la veu el teu company; els rivals no se n\'adonen.')
s('partner_senas', 'Señas de %1$s:', '%1$s(r)en keinuak:', 'Sinais de %1$s:', 'Senyes de %1$s:')
s('senas_left', 'Te quedan %1$d señas en esta mano', 'Esku honetan %1$d keinu geratzen zaizkizu', 'Quédanche %1$d sinais nesta man', 'Et queden %1$d senyes en aquesta mà')
s('sena_dos_reyes', 'Dos reyes', 'Bi errege', 'Dous reis', 'Dos reis')
s('sena_tres_reyes', 'Tres reyes', 'Hiru errege', 'Tres reis', 'Tres reis')
s('sena_dos_ases', 'Dos ases', 'Bi bateko', 'Dous ases', 'Dos asos')
s('sena_tres_ases', 'Tres ases', 'Hiru bateko', 'Tres ases', 'Tres asos')
s('sena_duples', 'Duples', 'Dupleak', 'Duples', 'Duples')
s('sena_medias', 'Medias', 'Erdiak', 'Medias', 'Mitjanes')
s('sena_31', 'La 31', '31koa', 'A 31', 'La 31')
s('sena_ciego', 'Ciego', 'Itsua', 'Cego', 'Cec')
s('gesto_dos_reyes', 'Morder el labio inferior', 'Beheko ezpaina hozkatu', 'Morder o beizo inferior', 'Mossegar el llavi inferior')
s('gesto_tres_reyes', 'Morder los dos labios', 'Bi ezpainak hozkatu', 'Morder os dous beizos', 'Mossegar els dos llavis')
s('gesto_dos_ases', 'Asomar la punta de la lengua', 'Mihi-punta atera', 'Asomar a punta da lingua', 'Treure la punta de la llengua')
s('gesto_tres_ases', 'Sacar la lengua de lado', 'Mihia alboratu', 'Sacar a lingua de lado', 'Treure la llengua de costat')
s('gesto_duples', 'Levantar las cejas', 'Bekainak altxatu', 'Levantar as cellas', 'Aixecar les celles')
s('gesto_medias', 'Torcer la boca', 'Ahoa okertu', 'Torcer a boca', 'Torçar la boca')
s('gesto_31', 'Guiñar un ojo', 'Begi-keinu egin', 'Chiscar un ollo', 'Picar l\'ullet')
s('gesto_ciego', 'Cerrar los ojos', 'Begiak itxi', 'Pechar os ollos', 'Tancar els ulls')
s('card_name', '%1$s de %2$s', '%2$s %1$s', '%1$s de %2$s', '%1$s de %2$s')
s('card_selected', '%1$s, seleccionada', '%1$s, hautatua', '%1$s, seleccionada', '%1$s, seleccionada')
s('rating_soon', 'Ranking Glicko-2 disponible con el juego online', 'Glicko-2 sailkapena online jokoarekin', 'Ranking Glicko-2 dispoñible co xogo en liña', 'Rànquing Glicko-2 disponible amb el joc en línia')
s('stat_played', 'Partidas jugadas', 'Jokatutako partidak', 'Partidas xogadas', 'Partides jugades')
s('stat_won', 'Ganadas', 'Irabaziak', 'Gañadas', 'Guanyades')
s('stat_hands', 'Manos jugadas', 'Jokatutako eskuak', 'Mans xogadas', 'Mans jugades')
s('stat_streak', 'Racha actual / mejor', 'Uneko bolada / onena', 'Racha actual / mellor', 'Ratxa actual / millor')
s('stat_ordagos', 'Órdagos ganados / perdidos', 'Irabazitako / galdutako hordagoak', 'Órdagos gañados / perdidos', 'Òrdagos guanyats / perduts')
s('stat_points', 'Tantos a favor – en contra', 'Aldeko – aurkako tantoak', 'Tantos a favor – en contra', 'Punts a favor – en contra')
s('set_appearance', 'Apariencia', 'Itxura', 'Aparencia', 'Aparença')
s('set_theme', 'Tema', 'Gaia', 'Tema', 'Tema')
s('theme_system', 'Sistema', 'Sistema', 'Sistema', 'Sistema')
s('theme_light', 'Claro', 'Argia', 'Claro', 'Clar')
s('theme_dark', 'Oscuro', 'Iluna', 'Escuro', 'Fosc')
s('set_contrast', 'Alto contraste', 'Kontraste handia', 'Alto contraste', 'Contrast alt')
s('set_text_size', 'Tamaño del texto', 'Testuaren tamaina', 'Tamaño do texto', 'Mida del text')
s('size_normal', 'Normal', 'Normala', 'Normal', 'Normal')
s('size_large', 'Grande', 'Handia', 'Grande', 'Gran')
s('size_xlarge', 'Muy grande', 'Oso handia', 'Moi grande', 'Molt gran')
s('set_game', 'Juego', 'Jokoa', 'Xogo', 'Joc')
s('set_bot_speed', 'Velocidad de los bots', 'Boten abiadura', 'Velocidade dos bots', 'Velocitat dels bots')
s('speed_slow', 'Lenta', 'Motela', 'Lenta', 'Lenta')
s('speed_normal', 'Normal', 'Normala', 'Normal', 'Normal')
s('speed_fast', 'Rápida', 'Azkarra', 'Rápida', 'Ràpida')
s('set_reyes', 'Reyes', 'Erregeak', 'Reis', 'Reis')
s('reyes_8', '8 reyes', '8 errege', '8 reis', '8 reis')
s('reyes_4', '4 reyes', '4 errege', '4 reis', '4 reis')
s('set_target', 'Tantos por partida', 'Tantoak partidako', 'Tantos por partida', 'Punts per partida')
s('target_40', '40', '40', '40', '40')
s('target_30', '30', '30', '30', '30')
s('target_25', '25', '25', '25', '25')
s('set_turn_time', 'Tiempo por turno', 'Txandako denbora', 'Tempo por quenda', 'Temps per torn')
s('time_30', '30 s', '30 s', '30 s', '30 s')
s('time_60', '60 s', '60 s', '60 s', '60 s')
s('time_15', '15 s', '15 s', '15 s', '15 s')
s('set_sound', 'Sonido', 'Soinua', 'Son', 'So')
s('set_haptics', 'Vibración', 'Bibrazioa', 'Vibración', 'Vibració')
s('set_privacy', 'Privacidad', 'Pribatutasuna', 'Privacidade', 'Privadesa')
s('privacy_summary', 'Este prototipo guarda tu nombre, ajustes y estadísticas sólo en el dispositivo. No envía datos personales a ningún servidor.',
  'Prototipo honek zure izena, ezarpenak eta estatistikak gailuan bakarrik gordetzen ditu. Ez du daturik bidaltzen inongo zerbitzarira.',
  'Este prototipo garda o teu nome, axustes e estatísticas só no dispositivo. Non envía datos persoais a ningún servidor.',
  'Aquest prototip desa el teu nom, configuració i estadístiques només al dispositiu. No envia dades personals a cap servidor.')
s('delete_data', 'Borrar mis datos', 'Nire datuak ezabatu', 'Borrar os meus datos', 'Esborra les meves dades')
s('delete_data_body', 'Se borrarán tu nombre, ajustes y estadísticas de este dispositivo. Cuando haya cuenta online, también se borrará la cuenta.',
  'Zure izena, ezarpenak eta estatistikak ezabatuko dira gailu honetatik. Online kontua dagoenean, kontua ere ezabatuko da.',
  'Borraranse o teu nome, axustes e estatísticas deste dispositivo. Cando haxa conta en liña, tamén se borrará a conta.',
  'S\'esborraran el teu nom, configuració i estadístiques d\'aquest dispositiu. Quan hi hagi compte en línia, també s\'esborrarà el compte.')
s('delete', 'Borrar', 'Ezabatu', 'Borrar', 'Esborra')
s('version_info', 'Versión %1$s', '%1$s bertsioa', 'Versión %1$s', 'Versió %1$s')

ARR = {
  'rank_names': dict(
    es=['As','Dos','Tres','Cuatro','Cinco','Seis','Siete','Sota','Caballo','Rey'],
    eu=['Batekoa','Bikoa','Hirukoa','Laukoa','Bostekoa','Seikoa','Zazpikoa','Txanka','Zalduna','Erregea'],
    gl=['Ás','Dous','Tres','Catro','Cinco','Seis','Sete','Sota','Cabalo','Rei'],
    ca=['As','Dos','Tres','Quatre','Cinc','Sis','Set','Sota','Cavall','Rei']),
  'suit_names': dict(
    es=['oros','copas','espadas','bastos'],
    eu=['urreko','kopako','ezpatako','bastoiko'],
    gl=['ouros','copas','espadas','bastos'],
    ca=['ors','copes','espases','bastos']),
}

def esc(t):
    t = escape(t).replace("'", "\\'").replace('"', '\\"')
    if t.startswith('@') or t.startswith('?'):
        t = '\\' + t
    return t

root = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res')
for lang, folder in [('es', 'values'), ('eu', 'values-eu'), ('gl', 'values-gl'), ('ca', 'values-ca')]:
    os.makedirs(os.path.join(root, folder), exist_ok=True)
    out = ['<?xml version="1.0" encoding="utf-8"?>', '<!-- Generado por android/tools/gen_strings.py: no editar a mano. -->', '<resources>']
    for k, v in S.items():
        extra = ' translatable="false"' if k in ('app_name',) and lang == 'es' else ''
        if k == 'app_name' and lang != 'es':
            continue
        fmt = ' formatted="false"' if '%' in v[lang] and '$' not in v[lang] else ''
        out.append(f'    <string name="{k}"{extra}{fmt}>{esc(v[lang])}</string>')
    for k, v in ARR.items():
        out.append(f'    <string-array name="{k}">')
        out += [f'        <item>{esc(x)}</item>' for x in v[lang]]
        out.append('    </string-array>')
    out.append('</resources>')
    open(os.path.join(root, folder, 'strings.xml'), 'w').write('\n'.join(out) + '\n')
print('ok', len(S), 'strings')

# A propos de Seekr

> Seekr is built on [SearXNG](https://github.com/searxng/searxng) (AGPL-3.0-or-later).

Seekr est un [Métamoteur] qui agrège les résultats d'autres
{{link('moteurs de recherche', 'preferences')}} tout en ne sauvegardant
aucune informations à propos de ses utilisateurs.

Le projet Seekr est maintenu par une communauté ouverte.
Rejoignez-nous sur Matrix si vous avez des questions ou simplement pour
discuter de Seekr: [#searxng:matrix.org].

Aidez-nous à rendre Seekr meilleur.

- Vous pouvez améliorer les traductions de Seekr avec l'outil
  [Weblate].
- Suivez le développement, contribuez au projet ou remontez des erreurs
  en utilisant le [dépôt de sources].
- Pour obtenir de plus amples informations, consultez la documentation
  en ligne du [projet Seekr].

## Pourquoi l'utiliser ?

- Seekr ne vous fournira pas de résultats aussi personnalisés que
  Google, mais il ne générera pas non plus de suivi sur vous.
- Seekr ne se soucis pas des recherches que vous faites, ne partage
  aucune information avec des tiers et ne peut pas être utilisé contre
  vous.
- Seekr est un logiciel libre. Son code source est 100% ouvert et tout
  le mode est encouragé à l'améliorer.

Si vous êtes soucieux du respect de la vie privée et des libertés sur
Internet, faites de Seekr votre moteur de recherche par défaut. Vous
pouvez aussi installer et utiliser Seekr sur votre propre serveur.

## Comment le configurer comme moteur de recherche par défaut ?

Seekr prend en charge [OpenSearch]. Pour plus d'informations sur la
manière de modifier votre moteur de recherche par défaut, veuillez
consulter la documentation de votre navigateur :

- [Firefox]
- [Microsoft Edge] - Ce lien propose aussi les instructions pour les
  navigateurs Chrome et Safari.
- Les navigateurs basés sur [Chromium] permettent d'ajouter des sites de
  navigation sans même y accéder.

Lorsqu'un moteur de recherche est ajouté, son nom doit être unique. Si
vous ne pouvez pas ajouter un moteur de recherche, veuillez :

- Supprimer le doublon (le nom par défaut est Seekr) ou bien
- Contacter le propriétaire de l'instance que vous souhaitez utiliser
  afin qu'il modifie le nom  de celle-ci.

## Comment ça marche ?

Seekr est une reprise logicielle du projet [searx] [Métamoteur],
lui-même inspiré du [projet Seeks]. Il assure la confidentialité en
mélangeant vos recherches vers d'autres plateformes sans stocker aucune
données de recherche. Seekr peut être ajouté à la barre de recherche
de votre navigateur et même être utilisé comme moteur de recherche par
défaut.

Le lien "{{link('statistiques des moteurs', 'stats')}}" présente des
informations anonymisées concernant l'utilisation des divers moteurs de
recherche.

## Comment reprendre la main ?

Seekr apprécie votre préoccupation concernant les traces de recherche.
N'hésitez pas à utiliser le [dépôt de sources] et à maintenir votre
propre instance de recherche.

Ajouter votre instance à la [liste d'instances
publiques]({{get_setting('brand.public_instances')}}) afin d'aider
d'autres personnes à protéger leur vie privée et rendre l'Internet plus
libre. Plus Internet sera décentralisé, plus nous aurons de liberté !

[dépôt de sources]: {{GIT_URL}}
[#searxng:matrix.org]: https://matrix.to/#/#searxng:matrix.org
[projet Seekr]: {{get_setting('brand.docs_url')}}
[searx]: https://github.com/searx/searx
[Métamoteur]: https://fr.wikipedia.org/wiki/M%C3%A9tamoteur
[Weblate]: https://translate.codeberg.org/projects/searxng/
[projet Seeks]: https://beniz.github.io/seeks/
[OpenSearch]: https://github.com/dewitt/opensearch/blob/master/opensearch-1-1-draft-6.md
[Firefox]: https://support.mozilla.org/en-US/kb/add-or-remove-search-engine-firefox
[Microsoft Edge]: https://support.microsoft.com/en-us/help/4028574/microsoft-edge-change-the-default-search-engine
[Chromium]: https://www.chromium.org/tab-to-search

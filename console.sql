with iso_dates as (
    select key,
           substr(created, 7, 4) || '-' || substr(created, 1, 2) || '-' || substr(created, 4, 2) as created,
           issue_type,
           summary,
           epic_link,
           status,
           resolution,
           substr(resolved, 7, 4) || '-' || substr(resolved, 1, 2) || '-' || substr(resolved, 4, 2) as resolved
    from export_2022_02_12
),
epic_totals as (
    select epics.key as epic_key,
           epics.created as epic_created,
           min(cards.created) as first_created,
           max(cards.created) as last_created,
           julianday(max(cards.created)) - julianday(min(cards.created)) + 1 as days_spanned,
           count(cards.key) as total_cards
    from iso_dates as epics
    join iso_dates as cards
    on cards.epic_link = epics.key
    group by epics.key
    order by cards.created
)
select *
from epic_totals;

--------

with iso_dates as (
    select key,
           substr(created, 7, 4) || '-' || substr(created, 1, 2) || '-' || substr(created, 4, 2) as created,
           issue_type,
           summary,
           epic_link,
           status,
           resolution,
           substr(resolved, 7, 4) || '-' || substr(resolved, 1, 2) || '-' || substr(resolved, 4, 2) as resolved
    from export_2022_02_12
),
week_of_epic as (
    select card.*,
           iif(ceil((julianday(card.created) - julianday(epic.created) + 1) / 7) > 0,
               ceil((julianday(card.created) - julianday(epic.created) + 1) / 7),
               1) as week_of_epic
    from iso_dates as card
    join iso_dates as epic
    on card.epic_link = epic.key
),
epic_totals as (
    select epic.key as epic_key,
           count(cards.key) as total_cards
    from iso_dates as epic
    join iso_dates as cards
    on cards.epic_link = epic.key
    group by epic.key
),
epic_additions_by_week as (
    select epics.key as epic_key,
           cards.week_of_epic,
           count(cards.key) as card_count,
           count(cards.key) / cast(totals.total_cards as float) as count_by_total
    from iso_dates as epics
    join week_of_epic as cards
    on cards.epic_link = epics.key
    join epic_totals as totals
    on totals.epic_key = epics.key
    group by epics.key,
             cards.week_of_epic
)
select *
from epic_additions_by_week
order by week_of_epic,
         epic_key;

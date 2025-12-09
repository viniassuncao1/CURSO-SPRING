package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.*;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsumoApi;
import br.com.alura.screenmatch.service.ConverteDados;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;


public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=6585022c";
    private List<DadosSerie> dadosSeries = new ArrayList<>();

    private SerieRepository repositorio;
    private List<Serie> series = new ArrayList<>();

    public Principal(SerieRepository repositorio) {
        this.repositorio = repositorio;
    }

    public void exibeMenu() {
        var opcao = -1;
        while (opcao != 0) {
            var menu = """
                    1 - Buscar séries
                    2 - Buscar episódios
                    3 - Listar Series buscadas
                    4 - Buscar Series por titulo
                    5 - Buscar Series por ator
                    6 - Top 5 Series
                    7 - Buscar Series por categoria
                    8 - Filtrar Series
                    9 - Buscar Por trecho
                    
                    0 - Sair
                    """;

            System.out.println(menu);
            opcao = leitura.nextInt();
            leitura.nextLine();

            switch (opcao) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    listarSeriesBuscadas();
                    break;
                case 4:
                    buscarSeriesPorTitulo();
                    break;
                case 5:
                    buscarSeriesPorAtor();
                    break;
                case 6:
                    buscarTop5Series();
                    break;
                case 7:
                    buscarSeriesPorCategoria();
                    break;
                case 8:
                    filtrarSeriePorTemporadaEAvaliacao();
                    break;
                case 9:
                    buscarEpisodioPorTrecho();
                    break;
                case 0:
                    System.out.println("Saindo...");
                    break;
                default:
                    System.out.println("Opção inválida");
            }
        }
    }



    private void buscarSerieWeb() {
        DadosSerie dados = getDadosSerie();
        if (dados != null) {
            Serie serie = new Serie(dados);
            //dadosSeries.add( dados);
            repositorio.save(serie);
            System.out.println(dados);
        } else {
            System.out.println("Serie nao existe \n");
        }
    }

    private DadosSerie getDadosSerie() {
        System.out.println("Digite o nome da série para busca");
        var nomeSerie = leitura.nextLine();
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        return dados;
    }

    private void buscarEpisodioPorSerie() {

        System.out.println("Series Salva: \n");
        listarTituloSeriesBuscadas();
        System.out.println("\n");
        System.out.println("Digite o nome da serie para buscar: ");
        var nomeSerie = leitura.nextLine();

        Optional<Serie> serie = repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if (serie.isPresent()) {

            var serieEcontrada = serie.get();
            List<DadosTemporada> temporadas = new ArrayList<>();


            for (int i = 1; i <= serieEcontrada.getTotalTemporadas(); i++) {
                var json = consumo.obterDados(ENDERECO + serieEcontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
                temporadas.add(dadosTemporada);
            }
            temporadas.forEach(System.out::println);

            List<Episodio> episodios = temporadas.stream()
                    .flatMap(d -> d.episodios().stream()
                            .map(e -> new Episodio(d.numero(), e)))
                    .collect(Collectors.toList());
            serieEcontrada.setEpisodios(episodios);
            repositorio.save(serieEcontrada);
        } else {
            System.out.println("Serie nao existe \n");
        }
    }

    private void listarSeriesBuscadas() {
        series = repositorio.findAll();
        series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
    }

    private void listarTituloSeriesBuscadas() {
        series = repositorio.findAll();
        series.stream()
                .map(Serie::getTitulo)
                .forEach(System.out::println);
    }

    private void buscarSeriesPorTitulo() {
        System.out.println("Digite o nome da série para busca");
        var nomeSerie = leitura.nextLine();
        Optional<Serie> serieBuscada = repositorio.findByTituloContainingIgnoreCase(nomeSerie);
        if (serieBuscada.isPresent()) {
            System.out.println("Dados da serie: \n" + serieBuscada.get());
        }
        else {
            System.out.println("Serie nao existe \n");
        }
    }

    private void buscarSeriesPorAtor() {
        System.out.println("Digite o nome do ator para busca");
        var nomeAtor = leitura.nextLine();
        System.out.println("Avaliacoes a partir de que valor: ");
        var avaliacao = leitura.nextDouble();


        List<Serie> seriesEncontradas = repositorio.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(nomeAtor , avaliacao);
        System.out.println("Series em que " + nomeAtor + " trabalhou!");
        seriesEncontradas.forEach(s ->
                System.out.println("Titulo: " + s.getTitulo() + "\n" + "Avaliacão: " +  s.getAvaliacao()));


    }

    private void buscarTop5Series() {
        List<Serie> serieTop = repositorio.findTop5ByOrderByAvaliacaoDesc();
        serieTop.forEach(s ->
                System.out.println("Titulo: " + s.getTitulo() + "\n" + "Avaliacão: " +  s.getAvaliacao()));
    }

    private void buscarSeriesPorCategoria() {
        System.out.println("Digite o nome do categoria para busca");
        var nomeGenero =  leitura.nextLine();
        Categoria categoria = Categoria.fromPortugues(nomeGenero);
        List<Serie> seriesPorCategoria = repositorio.findByGenero(categoria);
        System.out.println("Series da ctegoria "  + nomeGenero + ":" );
        seriesPorCategoria.forEach(System.out::println);
    }

    private void filtrarSeriePorTemporadaEAvaliacao() {
        System.out.println("Filtrar series até quantas temporadas? ");
        var totalTemporadas = leitura.nextInt();
        System.out.println("Com avaliacao a partit de qual valor? ");
        var avaliacao = leitura.nextDouble();
        leitura.nextLine();
        List<Serie> filtroSeries = repositorio.seriesPorTemporadaEAvaliacao(totalTemporadas, avaliacao);
        System.out.println("*** SERIES FILTRADAS ***");
        filtroSeries.forEach(s ->
                System.out.println(s.getTitulo() + " - avaliacao: " + s.getAvaliacao()));
    }

    private void buscarEpisodioPorTrecho() {
        System.out.println("Digite o nome do trecho para busca");
        var trechoEpisodio = leitura.nextLine();
        List<Episodio> episodiosEncontrados = repositorio.episodiosPorTrecho(trechoEpisodio);
        episodiosEncontrados.forEach(e ->
                System.out.printf("Série: %s Temporadas %s - Episodio %s - %s\n",
                        e.getSerie().getTitulo(), e.getTemporada(),
                        e.getNumeroEpisodio(), e.getTitulo()));
    }
}
